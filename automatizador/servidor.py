"""Interfaz local para preparar y enviar el lote, un correo por persona."""

from __future__ import annotations

import argparse
import csv
import io
import json
import threading
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

from automatizador.campana import (
    Campana,
    archivar_lote_anterior,
    correos_enviados,
    estado_vacio,
    fabrica_por_defecto,
    vista_en_disco,
)
from automatizador.correo import (
    LIMITE_DESTINATARIOS,
    CuentaSmtp,
    ErrorAutenticacion,
    ErrorCorreo,
    ErrorEnvio,
    ErrorTransitorio,
    Mensaje,
    construir_mensaje,
    construir_vista,
    cuenta_de_envio,
)
from automatizador.destinatarios import es_email, parse_destinatarios
from automatizador.entorno import cargar_entornos, clave_del_entorno, configuracion_publica

STATIC = Path(__file__).resolve().parent / "static"
TIPOS = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".svg": "image/svg+xml",
    ".txt": "text/plain; charset=utf-8",
    ".csv": "text/csv; charset=utf-8",
}


class EstadoApp:
    def __init__(self, directorio: Path, fabrica=None) -> None:
        self.directorio = directorio
        self.directorio.mkdir(parents=True, exist_ok=True)
        self.fabrica = fabrica or fabrica_por_defecto
        self.lock = threading.Lock()
        self.campana: Campana | None = None
        self.hilo: threading.Thread | None = None
        self._reservado = False
        self.disco = vista_en_disco(directorio)

    def en_curso(self) -> bool:
        with self.lock:
            hilo = self.hilo
            reservado = self._reservado
        return reservado or (hilo is not None and hilo.is_alive())

    def ocupar(self) -> bool:
        with self.lock:
            if self._reservado or (self.hilo is not None and self.hilo.is_alive()):
                return False
            self._reservado = True
            return True

    def liberar_reserva(self) -> None:
        with self.lock:
            self._reservado = False

    def snapshot(self) -> dict:
        with self.lock:
            campana = self.campana
            disco = self.disco
        if campana is not None:
            return campana.snapshot()
        if disco is not None:
            return disco
        return estado_vacio()

    def iniciar(self, campana: Campana) -> None:
        with self.lock:
            if self.hilo is not None and self.hilo.is_alive():
                self._reservado = False
                raise RuntimeError("Ya hay un envío en curso.")
            self.campana = campana
            self.disco = None
            self.hilo = threading.Thread(target=campana.ejecutar, name="envio-correos", daemon=False)
            self.hilo.start()
            self._reservado = False

    def detener(self) -> bool:
        with self.lock:
            campana = self.campana
        if campana is None or not self.en_curso():
            return False
        campana.detener.set()
        return True


class ServidorCorreos(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def __init__(self, direccion, estado: EstadoApp) -> None:
        self.estado = estado
        super().__init__(direccion, Manejador)


class Manejador(BaseHTTPRequestHandler):
    server: ServidorCorreos

    def do_GET(self) -> None:
        try:
            ruta = unquote(urlparse(self.path).path)
            if ruta == "/api/salud":
                self._json(200, {"ok": True})
                return
            if ruta == "/api/estado":
                self._json(200, self.server.estado.snapshot())
                return
            if ruta == "/api/configuracion":
                self._json(200, configuracion_publica())
                return
            if ruta == "/api/resultados.csv":
                self._csv()
                return
            self._estatico(ruta)
        except Exception:
            traceback.print_exc()
            self._json(500, {"mensaje": "Error interno del automatizador."})

    def do_POST(self) -> None:
        try:
            self._post()
        except Exception:
            traceback.print_exc()
            self._json(500, {"mensaje": "Error interno del automatizador."})

    def _post(self) -> None:
        ruta = unquote(urlparse(self.path).path)
        try:
            datos = self._leer_json()
        except ValueError as exc:
            self._json(400, {"mensaje": str(exc)})
            return
        if ruta == "/api/revisar":
            if len(str(datos.get("destinatarios") or "")) > 1_000_000:
                self._json(400, {"mensaje": "La lista es demasiado larga."})
                return
            self._json(200, revisar(datos))
            return
        if ruta == "/api/simular":
            self._lanzar(datos, simular=True)
            return
        if ruta == "/api/enviar":
            self._lanzar(datos, simular=False)
            return
        if ruta == "/api/prueba":
            self._prueba(datos)
            return
        if ruta == "/api/detener":
            detuvo = self.server.estado.detener()
            if detuvo:
                self._json(200, {"mensaje": "Se detendrá al terminar el correo que va en curso."})
            else:
                self._json(200, {"mensaje": "No hay un envío en curso."})
            return
        self._json(404, {"mensaje": "No existe esa acción."})

    def _lanzar(self, datos: dict, simular: bool) -> None:
        estado = self.server.estado
        if not estado.ocupar():
            self._json(409, {"mensaje": "Ya hay un envío en curso."})
            return
        try:
            preparado = preparar_lote(datos, simular=simular, directorio=estado.directorio)
        except ValueError as exc:
            estado.liberar_reserva()
            self._json(400, {"mensaje": str(exc)})
            return
        campana = Campana(
            preparado["destinatarios"],
            preparado["mensaje"],
            preparado["cuenta"],
            estado.fabrica(preparado["cuenta"], simular),
            pausa=preparado["pausa"],
            simular=simular,
            ya_enviados=preparado["ya_enviados"],
            ruta_jsonl=preparado["ruta_jsonl"],
            ruta_meta=preparado["ruta_meta"],
        )
        try:
            estado.iniciar(campana)
        except RuntimeError as exc:
            estado.liberar_reserva()
            self._json(409, {"mensaje": str(exc)})
            return
        verbo = "Simulación iniciada" if simular else "Envío iniciado"
        self._json(200, {"mensaje": f"{verbo}. {preparado['destinatarios_total']} correos, uno por persona."})

    def _prueba(self, datos: dict) -> None:
        estado = self.server.estado
        if estado.en_curso():
            self._json(409, {"mensaje": "Espera a que termine el envío en curso."})
            return
        destino = str(datos.get("destino_prueba") or "").strip()
        if not es_email(destino):
            self._json(400, {"mensaje": "Escribe un correo válido para la prueba."})
            return
        try:
            cuenta = cuenta_desde_datos(datos, exigir=True)
            mensaje = mensaje_desde_datos(datos)
        except ValueError as exc:
            self._json(400, {"mensaje": str(exc)})
            return
        from automatizador.destinatarios import Destinatario

        persona = Destinatario(destino, "Prueba")
        transporte = estado.fabrica(cuenta, False)
        try:
            transporte.conectar()
            correo = construir_mensaje(cuenta_de_envio(cuenta, False), mensaje, persona, 1, 1)
            transporte.enviar(correo)
        except ErrorAutenticacion as exc:
            self._json(400, {"mensaje": str(exc)})
            return
        except (ErrorEnvio, ErrorTransitorio, ErrorCorreo) as exc:
            self._json(400, {"mensaje": str(exc)})
            return
        finally:
            transporte.cerrar()
        self._json(200, {"mensaje": f"Prueba enviada solo a {destino}."})

    def _leer_json(self) -> dict:
        crudo_largo = self.headers.get("Content-Length", "0")
        try:
            largo = int(crudo_largo)
        except ValueError as exc:
            raise ValueError("No se pudo leer el tamaño del pedido.") from exc
        if largo < 0 or largo > 2_000_000:
            raise ValueError("El pedido es demasiado grande.")
        cuerpo = self.rfile.read(largo) if largo else b""
        if not cuerpo:
            return {}
        try:
            datos = json.loads(cuerpo.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ValueError("El pedido no es un JSON válido.") from exc
        if not isinstance(datos, dict):
            raise ValueError("El pedido no es un JSON válido.")
        return datos

    def _estatico(self, ruta: str) -> None:
        relativo = "index.html" if ruta == "/" else ruta.lstrip("/")
        if not relativo or "\\" in relativo:
            self._json(404, {"mensaje": "No se encontró la página."})
            return
        base = STATIC.resolve()
        destino = (base / relativo).resolve()
        if destino != base and base not in destino.parents:
            self._json(404, {"mensaje": "No se encontró la página."})
            return
        if not destino.is_file():
            self._json(404, {"mensaje": "No se encontró la página."})
            return
        contenido = destino.read_bytes()
        tipo = TIPOS.get(destino.suffix, "application/octet-stream")
        self._bytes(200, contenido, tipo)

    def _csv(self) -> None:
        filas = self.server.estado.snapshot().get("resultados") or []
        buffer = io.StringIO()
        escritor = csv.DictWriter(
            buffer,
            fieldnames=["orden", "hora", "email", "nombre", "estado", "detalle"],
            extrasaction="ignore",
        )
        escritor.writeheader()
        for fila in filas:
            if isinstance(fila, dict):
                escritor.writerow(fila)
        self._bytes(200, buffer.getvalue().encode("utf-8"), "text/csv; charset=utf-8")

    def _json(self, codigo: int, payload: dict) -> None:
        self._bytes(
            codigo,
            json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            "application/json; charset=utf-8",
        )

    def _bytes(self, codigo: int, cuerpo: bytes, tipo: str) -> None:
        try:
            self.send_response(codigo)
            self.send_header("Content-Type", tipo)
            self.send_header("Content-Length", str(len(cuerpo)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header(
                "Content-Security-Policy",
                "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self'; "
                "base-uri 'none'; form-action 'self'; frame-ancestors 'none'",
            )
            self.end_headers()
            self.wfile.write(cuerpo)
        except (BrokenPipeError, ConnectionResetError):
            return

    def log_message(self, formato: str, *args) -> None:
        return


def revisar(datos: dict) -> dict:
    texto = str(datos.get("destinatarios") or "")
    lista = parse_destinatarios(texto)
    mensaje = Mensaje(
        asunto=str(datos.get("asunto") or ""),
        texto=str(datos.get("texto") or ""),
        html=str(datos.get("html") or ""),
    )
    vista = construir_vista(lista.validos, mensaje) if mensaje.asunto.strip() or mensaje.texto.strip() or mensaje.html.strip() else {
        "identico": True,
        "muestras_mensaje": [],
    }
    validos = [{"email": d.email, "nombre": d.nombre} for d in lista.validos]
    cierre = validos[-2:] if len(validos) > 8 else []
    return {
        "total": len(lista.validos),
        "invalidos": lista.invalidos[:50],
        "invalidos_total": len(lista.invalidos),
        "duplicados": lista.duplicados[:50],
        "duplicados_total": len(lista.duplicados),
        "muestra": validos[:8],
        "cierre": cierre,
        "identico": bool(vista["identico"]) if lista.validos else True,
        "muestras_mensaje": vista["muestras_mensaje"] if lista.validos else [],
    }


def preparar_lote(datos: dict, *, simular: bool, directorio: Path) -> dict:
    texto = str(datos.get("destinatarios") or "")
    if len(texto) > 1_000_000:
        raise ValueError("La lista es demasiado larga.")
    lista = parse_destinatarios(texto)
    if not lista.validos:
        raise ValueError("No hay correos válidos en la lista.")
    if len(lista.validos) > LIMITE_DESTINATARIOS:
        raise ValueError(f"El máximo de este automatizador es {LIMITE_DESTINATARIOS} correos por lote.")
    mensaje = mensaje_desde_datos(datos)
    if not simular:
        confirmar = datos.get("confirmar")
        if str(confirmar) != str(len(lista.validos)):
            raise ValueError(
                f"Para enviar hay que confirmar el número exacto: {len(lista.validos)}."
            )
        cuenta = cuenta_desde_datos(datos, exigir=True)
        reanudar = bool(datos.get("reanudar"))
        jsonl = directorio / "ultimo.jsonl"
        if reanudar:
            ya = correos_enviados(jsonl)
        else:
            archivar_lote_anterior(directorio)
            ya = set()
        ruta_jsonl = jsonl
        ruta_meta = directorio / "ultimo.meta.json"
    else:
        cuenta = cuenta_desde_datos(datos, exigir=False)
        ya = set()
        ruta_jsonl = None
        ruta_meta = None
    return {
        "destinatarios": lista.validos,
        "destinatarios_total": len(lista.validos),
        "mensaje": mensaje,
        "cuenta": cuenta,
        "pausa": _pausa(datos.get("pausa")),
        "ya_enviados": ya,
        "ruta_jsonl": ruta_jsonl,
        "ruta_meta": ruta_meta,
    }


def mensaje_desde_datos(datos: dict) -> Mensaje:
    mensaje = Mensaje(
        asunto=str(datos.get("asunto") or ""),
        texto=str(datos.get("texto") or ""),
        html=str(datos.get("html") or ""),
    )
    errores = mensaje.validar()
    if errores:
        raise ValueError(errores[0])
    return mensaje


def cuenta_desde_datos(datos: dict, *, exigir: bool) -> CuentaSmtp:
    usuario = str(datos.get("usuario") or "").strip()
    remitente = str(datos.get("remitente") or "").strip() or usuario
    if datos.get("usar_clave_entorno"):
        clave = clave_del_entorno()
    else:
        clave = str(datos.get("clave") or "")
    cuenta = CuentaSmtp(
        host=str(datos.get("host") or "").strip(),
        port=_entero(datos.get("puerto"), 587),
        usuario=usuario,
        clave=clave,
        remitente=remitente,
        nombre_remitente=str(datos.get("nombre_remitente") or "").strip(),
        cifrado=str(datos.get("cifrado") or "starttls"),
    )
    if exigir:
        errores = cuenta.validar()
        if errores:
            raise ValueError(errores[0])
    return cuenta


def _entero(valor, defecto: int) -> int:
    try:
        return int(valor)
    except (TypeError, ValueError):
        return defecto


def _pausa(valor) -> float:
    try:
        pausa = float(valor)
    except (TypeError, ValueError):
        return 2
    if pausa < 0:
        return 0
    if pausa > 120:
        return 120
    return pausa


def crear_servidor(
    directorio: Path,
    host: str = "127.0.0.1",
    puerto: int = 0,
    fabrica=None,
) -> ServidorCorreos:
    estado = EstadoApp(directorio, fabrica)
    return ServidorCorreos((host, puerto), estado)


def main(argv: list[str] | None = None) -> int:
    cargar_entornos()
    parser = argparse.ArgumentParser(description="Abre el automatizador de correos en este computador.")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--puerto", type=int, default=8765)
    parser.add_argument("--salidas", default="salidas")
    args = parser.parse_args(argv)
    if args.host not in {"127.0.0.1", "localhost", "::1"}:
        print("El automatizador solo escucha en este computador (127.0.0.1).")
        return 2
    host = "127.0.0.1" if args.host == "localhost" else args.host
    try:
        httpd = crear_servidor(Path(args.salidas), host, args.puerto)
    except OSError as exc:
        print(f"No se pudo abrir el puerto {args.puerto}: {exc}")
        return 1
    puerto = httpd.server_address[1]
    print(f"Automatizador listo en http://127.0.0.1:{puerto}")
    print("Déjalo abierto mientras se envían los correos. Ctrl+C lo detiene.")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido.")
    finally:
        httpd.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
