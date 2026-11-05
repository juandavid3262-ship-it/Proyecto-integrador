"""Recorre la lista y entrega un correo por destinatario."""

from __future__ import annotations

import json
import threading
import time
import traceback
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path

from automatizador.correo import (
    REINTENTOS,
    CuentaSmtp,
    ErrorAutenticacion,
    ErrorEnvio,
    ErrorTransitorio,
    Mensaje,
    TransporteSimulado,
    TransporteSmtp,
    construir_mensaje,
    cuenta_de_envio,
)
from automatizador.correo import ZONA
from automatizador.destinatarios import Destinatario


@dataclass(frozen=True)
class Resultado:
    orden: int
    email: str
    nombre: str
    estado: str
    detalle: str
    hora: str


def ahora_bogota() -> str:
    return datetime.now(ZONA).strftime("%Y-%m-%d %H:%M:%S")


def estado_vacio() -> dict:
    return {
        "fase": "idle",
        "mensaje": "",
        "asunto": "",
        "total": 0,
        "procesados": 0,
        "enviados": 0,
        "fallidos": 0,
        "omitidos": 0,
        "simulados": 0,
        "actual": "",
        "inicio": "",
        "fin": "",
        "resultados": [],
    }


def leer_jsonl(ruta: Path) -> list[dict]:
    if not ruta.is_file():
        return []
    filas: list[dict] = []
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea:
            continue
        try:
            obj = json.loads(linea)
        except json.JSONDecodeError:
            continue
        if isinstance(obj, dict) and isinstance(obj.get("email"), str) and isinstance(obj.get("estado"), str):
            filas.append(obj)
    return filas


def correos_enviados(ruta: Path) -> set[str]:
    return {
        fila["email"].casefold()
        for fila in leer_jsonl(ruta)
        if fila.get("estado") == "enviado"
    }


def archivar_lote_anterior(directorio: Path) -> None:
    jsonl = directorio / "ultimo.jsonl"
    meta = directorio / "ultimo.meta.json"
    if not jsonl.exists() and not meta.exists():
        return
    marca = datetime.now(ZONA).strftime("%Y%m%d-%H%M%S")
    destino_jsonl = directorio / f"envio-{marca}.jsonl"
    sufijo = 2
    while destino_jsonl.exists():
        destino_jsonl = directorio / f"envio-{marca}-{sufijo}.jsonl"
        sufijo += 1
    marca_final = destino_jsonl.stem.replace("envio-", "")
    if jsonl.exists():
        jsonl.rename(destino_jsonl)
    if meta.exists():
        meta.rename(directorio / f"envio-{marca_final}.meta.json")


def vista_en_disco(directorio: Path) -> dict | None:
    jsonl = directorio / "ultimo.jsonl"
    meta_ruta = directorio / "ultimo.meta.json"
    if not jsonl.exists() and not meta_ruta.exists():
        return None
    filas = leer_jsonl(jsonl)
    meta: dict = {}
    if meta_ruta.is_file():
        try:
            cargada = json.loads(meta_ruta.read_text(encoding="utf-8"))
            if isinstance(cargada, dict):
                meta = cargada
        except json.JSONDecodeError:
            meta = {}
    fase = str(meta.get("fase") or "done")
    mensaje = str(meta.get("mensaje") or "")
    if fase == "running":
        fase = "stopped"
        mensaje = "El envío se interrumpió. Puedes reanudarlo para no repetir los que ya salieron."
    try:
        total_meta = int(meta.get("total") or len(filas))
    except (TypeError, ValueError):
        total_meta = len(filas)
    contadores = {"enviado": 0, "fallido": 0, "omitido": 0, "simulado": 0}
    for fila in filas:
        estado = fila.get("estado")
        if estado in contadores:
            contadores[estado] += 1
    vista = estado_vacio()
    vista.update(
        {
            "fase": fase,
            "mensaje": mensaje,
            "asunto": str(meta.get("asunto") or ""),
            "total": total_meta,
            "procesados": len(filas),
            "enviados": contadores["enviado"],
            "fallidos": contadores["fallido"],
            "omitidos": contadores["omitido"],
            "simulados": contadores["simulado"],
            "inicio": str(meta.get("inicio") or ""),
            "fin": str(meta.get("fin") or ""),
            "resultados": filas,
        }
    )
    return vista


class Campana:
    def __init__(
        self,
        destinatarios: list[Destinatario],
        mensaje: Mensaje,
        cuenta: CuentaSmtp,
        transporte,
        *,
        pausa: float = 2,
        simular: bool = False,
        ya_enviados: set[str] | None = None,
        ruta_jsonl: Path | None = None,
        ruta_meta: Path | None = None,
        dormir=time.sleep,
        espera_reintento: float = 5,
        al_resultado=None,
    ) -> None:
        self.destinatarios = destinatarios
        self.mensaje = mensaje
        self.cuenta = cuenta
        self.transporte = transporte
        self.pausa = max(0, float(pausa))
        self.simular = simular
        self.ya_enviados = ya_enviados or set()
        self.ruta_jsonl = ruta_jsonl
        self.ruta_meta = ruta_meta
        self._dormir = dormir
        self.espera_reintento = espera_reintento
        self.al_resultado = al_resultado
        self.fase = "idle"
        self.mensaje_estado = ""
        self.total = len(destinatarios)
        self.procesados = 0
        self.enviados = 0
        self.fallidos = 0
        self.omitidos = 0
        self.simulados = 0
        self.actual = ""
        self.inicio = ""
        self.fin = ""
        self.resultados: list[Resultado] = []
        self.lock = threading.Lock()
        self.detener = threading.Event()

    def snapshot(self) -> dict:
        with self.lock:
            return {
                "fase": self.fase,
                "mensaje": self.mensaje_estado,
                "asunto": self.mensaje.asunto,
                "total": self.total,
                "procesados": self.procesados,
                "enviados": self.enviados,
                "fallidos": self.fallidos,
                "omitidos": self.omitidos,
                "simulados": self.simulados,
                "actual": self.actual,
                "inicio": self.inicio,
                "fin": self.fin,
                "resultados": [asdict(resultado) for resultado in self.resultados],
            }

    def ejecutar(self) -> None:
        with self.lock:
            self.fase = "running"
            self.mensaje_estado = ""
            self.inicio = ahora_bogota()
        self._guardar_meta()
        try:
            if not self._conectar():
                return
            self._bucle()
        except Exception:
            traceback.print_exc()
            with self.lock:
                sigue = self.fase == "running"
            if sigue:
                self._terminar("error", "Error inesperado al enviar. No se continuó con el resto.")
        finally:
            try:
                self.transporte.cerrar()
            except Exception:
                traceback.print_exc()
            with self.lock:
                sigue = self.fase == "running"
            if sigue:
                self._terminar("done", self._resumen())

    def _conectar(self) -> bool:
        try:
            self.transporte.conectar()
            return True
        except ErrorAutenticacion as exc:
            self._terminar("error", str(exc))
            return False
        except ErrorTransitorio:
            try:
                self.transporte.conectar()
                return True
            except ErrorAutenticacion as exc:
                self._terminar("error", str(exc))
                return False
            except ErrorTransitorio:
                self._terminar("error", "No se pudo conectar al servidor de correo.")
                return False

    def _bucle(self) -> None:
        total = len(self.destinatarios)
        for indice, dest in enumerate(self.destinatarios, start=1):
            if self.detener.is_set():
                self._terminar("stopped", "Envío detenido. Los que faltan no se enviaron.")
                return
            with self.lock:
                self.actual = dest.email
            if dest.clave in self.ya_enviados:
                self._anotar(dest, indice, "omitido", "Ya se había enviado en este lote.")
                continue
            estado = self._enviar_con_reintentos(dest, indice, total)
            if estado == "fatal":
                return
            if self.detener.is_set():
                self._terminar("stopped", "Envío detenido. Los que faltan no se enviaron.")
                return
            if indice < total and self.pausa > 0:
                self._dormir(self.pausa)

    def _enviar_con_reintentos(self, dest: Destinatario, indice: int, total: int) -> str:
        ultimo = "No se pudo enviar."
        for intento in range(1, REINTENTOS + 1):
            if self.detener.is_set():
                return "detenido"
            try:
                correo = construir_mensaje(
                    cuenta_de_envio(self.cuenta, self.simular),
                    self.mensaje,
                    dest,
                    indice,
                    total,
                )
                self.transporte.enviar(correo)
            except ErrorAutenticacion as exc:
                self._terminar("error", str(exc))
                return "fatal"
            except ErrorEnvio as exc:
                self._anotar(dest, indice, "fallido", str(exc))
                return "fallido"
            except ErrorTransitorio as exc:
                ultimo = str(exc)
                if intento < REINTENTOS and not self.detener.is_set():
                    self._dormir(self.espera_reintento)
                    continue
                self._anotar(dest, indice, "fallido", ultimo)
                return "fallido"
            else:
                if self.simular:
                    self._anotar(dest, indice, "simulado", "Simulación: no se conectó a ningún servidor.")
                    return "simulado"
                self._anotar(dest, indice, "enviado", "Enviado solo a esta persona.")
                return "enviado"
        self._anotar(dest, indice, "fallido", ultimo)
        return "fallido"

    def _anotar(self, dest: Destinatario, orden: int, estado: str, detalle: str) -> None:
        resultado = Resultado(orden, dest.email, dest.nombre, estado, detalle, ahora_bogota())
        with self.lock:
            self.resultados.append(resultado)
            self.procesados += 1
            if estado == "enviado":
                self.enviados += 1
            elif estado == "fallido":
                self.fallidos += 1
            elif estado == "omitido":
                self.omitidos += 1
            elif estado == "simulado":
                self.simulados += 1
            ruta = self.ruta_jsonl
        if ruta is not None:
            ruta.parent.mkdir(parents=True, exist_ok=True)
            with ruta.open("a", encoding="utf-8") as archivo:
                archivo.write(json.dumps(asdict(resultado), ensure_ascii=False) + "\n")
        if self.al_resultado is not None:
            self.al_resultado(resultado)

    def _resumen(self) -> str:
        with self.lock:
            enviados = self.enviados
            fallidos = self.fallidos
            omitidos = self.omitidos
            simulados = self.simulados
        if simulados and not enviados:
            return (
                f"Simulación lista: {simulados} correos, cada uno por separado. "
                "No se envió nada."
            )
        partes = [f"{enviados} enviados"]
        if fallidos:
            partes.append(f"{fallidos} fallidos")
        if omitidos:
            partes.append(f"{omitidos} omitidos")
        return "Listo. " + ", ".join(partes) + "."

    def _terminar(self, fase: str, mensaje: str) -> None:
        with self.lock:
            self.fase = fase
            self.mensaje_estado = mensaje
            self.actual = ""
            self.fin = ahora_bogota()
        self._guardar_meta()

    def _guardar_meta(self) -> None:
        if self.ruta_meta is None:
            return
        with self.lock:
            payload = {
                "fase": self.fase,
                "mensaje": self.mensaje_estado,
                "asunto": self.mensaje.asunto,
                "total": self.total,
                "inicio": self.inicio,
                "fin": self.fin,
            }
        self.ruta_meta.parent.mkdir(parents=True, exist_ok=True)
        self.ruta_meta.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def fabrica_por_defecto(cuenta: CuentaSmtp, simular: bool):
    if simular:
        return TransporteSimulado()
    return TransporteSmtp(cuenta)
