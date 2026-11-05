"""Envío por terminal: el mismo correo, un destinatario a la vez."""

from __future__ import annotations

import argparse
import getpass
import os
import sys
from pathlib import Path

from automatizador.campana import Campana, archivar_lote_anterior, correos_enviados
from automatizador.correo import (
    LIMITE_DESTINATARIOS,
    CuentaSmtp,
    Mensaje,
    TransporteSimulado,
    TransporteSmtp,
    construir_vista,
)
from automatizador.destinatarios import leer_archivo, parse_destinatarios
from automatizador.entorno import cargar_entornos, clave_del_entorno

SALIDAS = Path("salidas")


def main(argv: list[str] | None = None) -> int:
    cargar_entornos()
    parser = argparse.ArgumentParser(
        description="Envía el mismo correo de forma individual a cada destinatario."
    )
    parser.add_argument("--destinatarios", required=True, help="CSV o texto, un correo por línea.")
    parser.add_argument("--mensaje", required=True, help="Archivo con el cuerpo del correo.")
    parser.add_argument("--html", help="Archivo HTML opcional.")
    parser.add_argument("--asunto", required=True)
    parser.add_argument("--pausa", type=float, default=2, help="Segundos entre un correo y el siguiente.")
    parser.add_argument("--simular", action="store_true", help="No se conecta. Muestra el lote.")
    parser.add_argument("--enviar", action="store_true", help="Envía de verdad.")
    parser.add_argument("--reanudar", action="store_true", help="Salta los que ya figuran como enviados.")
    parser.add_argument("--servidor", default=os.environ.get("SMTP_SERVIDOR", "smtp.gmail.com"))
    parser.add_argument("--puerto", type=int, default=int(os.environ.get("SMTP_PUERTO", "587")))
    parser.add_argument("--usuario", default=os.environ.get("SMTP_USUARIO", ""))
    parser.add_argument("--remitente", default=os.environ.get("SMTP_REMITENTE", ""))
    parser.add_argument("--nombre", default=os.environ.get("SMTP_NOMBRE", ""))
    parser.add_argument("--cifrado", choices=["starttls", "ssl", "ninguno"], default=os.environ.get("SMTP_CIFRADO", "starttls"))
    parser.add_argument("--salidas", default="salidas")
    args = parser.parse_args(argv)

    if args.simular == args.enviar:
        print("Elige una sola opción: --simular o --enviar.")
        return 2

    ruta_lista = Path(args.destinatarios)
    ruta_mensaje = Path(args.mensaje)
    if not ruta_lista.is_file() or not ruta_mensaje.is_file():
        print("No se encontró la lista o el archivo del mensaje.")
        return 2
    lista = parse_destinatarios(leer_archivo(ruta_lista))
    if not lista.validos:
        print("No hay correos válidos en la lista.")
        _imprimir_problemas(lista.invalidos)
        return 2
    if len(lista.validos) > LIMITE_DESTINATARIOS:
        print(f"El máximo de este automatizador es {LIMITE_DESTINATARIOS} correos por lote.")
        return 2

    html = leer_archivo(Path(args.html)) if args.html else ""
    mensaje = Mensaje(args.asunto, leer_archivo(ruta_mensaje), html)
    errores = mensaje.validar()
    if errores:
        print(errores[0])
        return 2

    vista = construir_vista(lista.validos, mensaje)
    print(f"Destinatarios válidos: {len(lista.validos)}")
    if lista.invalidos:
        print(f"Líneas omitidas: {len(lista.invalidos)}")
    if lista.duplicados:
        print(f"Repetidos (se usan una vez): {len(lista.duplicados)}")
    if vista["identico"]:
        print("Contenido: idéntico para todas las personas.")
    else:
        print("Contenido: el mismo texto, con {{nombre}}, {{email}}, {{n}} o {{total}} resueltos por persona.")
    if vista["muestras_mensaje"]:
        muestra = vista["muestras_mensaje"][0]
        print(f"Asunto: {muestra['asunto']}")

    if args.simular:
        cuenta = _cuenta(args, clave="", exigir=False)
        return _ejecutar(args, lista.validos, mensaje, cuenta, simular=True, ya_enviados=set())

    if not _confirmar(len(lista.validos)):
        print("No se envió nada.")
        return 2
    try:
        clave = _leer_clave()
    except SystemExit as exc:
        print(exc)
        return 2
    cuenta = _cuenta(args, clave=clave, exigir=True)
    errores_cuenta = cuenta.validar()
    if errores_cuenta:
        print(errores_cuenta[0])
        return 2

    directorio = Path(args.salidas)
    directorio.mkdir(parents=True, exist_ok=True)
    jsonl = directorio / "ultimo.jsonl"
    if args.reanudar:
        ya = correos_enviados(jsonl)
    else:
        archivar_lote_anterior(directorio)
        ya = set()
    return _ejecutar(args, lista.validos, mensaje, cuenta, simular=False, ya_enviados=ya)


def _ejecutar(args, destinatarios, mensaje, cuenta, *, simular: bool, ya_enviados: set[str]) -> int:
    directorio = Path(args.salidas)
    if simular:
        transporte = TransporteSimulado()
        ruta_jsonl = None
        ruta_meta = None
    else:
        transporte = TransporteSmtp(cuenta)
        ruta_jsonl = directorio / "ultimo.jsonl"
        ruta_meta = directorio / "ultimo.meta.json"

    def mostrar(resultado) -> None:
        print(
            f"[{resultado.orden}/{len(destinatarios)}] {resultado.estado}: {resultado.email} — {resultado.detalle}",
            flush=True,
        )

    campana = Campana(
        destinatarios,
        mensaje,
        cuenta,
        transporte,
        pausa=max(0, min(args.pausa, 120)),
        simular=simular,
        ya_enviados=ya_enviados,
        ruta_jsonl=ruta_jsonl,
        ruta_meta=ruta_meta,
        al_resultado=mostrar,
    )
    campana.ejecutar()
    print(campana.mensaje_estado)
    if campana.fase == "error" or campana.fallidos:
        return 1
    return 0


def _cuenta(args, *, clave: str, exigir: bool) -> CuentaSmtp:
    remitente = args.remitente or args.usuario or ("simulacion@local.test" if not exigir else "")
    return CuentaSmtp(
        host=args.servidor,
        port=args.puerto,
        usuario=args.usuario,
        clave=clave,
        remitente=remitente,
        nombre_remitente=args.nombre,
        cifrado=args.cifrado,
    )


def _confirmar(total: int) -> bool:
    print(f"Se enviarían {total} correos individuales.")
    print("Cada destinatario va solo en Para. El asunto y el cuerpo son los mismos.")
    if sys.stdin.isatty():
        escrito = input(f"Escribe {total} para enviar: ").strip()
    else:
        escrito = os.environ.get("CONFIRMAR_CANTIDAD", "").strip()
    return escrito == str(total)


def _leer_clave() -> str:
    clave = clave_del_entorno()
    if clave:
        return clave
    if not sys.stdin.isatty():
        raise SystemExit("Define SMTP_CLAVE en el entorno. No pongas la clave en el comando.")
    return getpass.getpass("Clave SMTP: ")


def _imprimir_problemas(invalidos: list[str]) -> None:
    for linea in invalidos[:20]:
        print(f"  - {linea}")


if __name__ == "__main__":
    raise SystemExit(main())
