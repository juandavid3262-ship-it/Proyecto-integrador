"""Carga un .env local sin pisar variables ya definidas ni leer claves de más."""

from __future__ import annotations

import os
from pathlib import Path

CLAVES_PERMITIDAS = {
    "SMTP_USUARIO",
    "SMTP_CLAVE",
    "SMTP_SERVIDOR",
    "SMTP_PUERTO",
    "SMTP_REMITENTE",
    "SMTP_NOMBRE",
    "SMTP_CIFRADO",
}


def cargar_env(ruta: Path) -> None:
    if not ruta.is_file():
        return
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea or linea.startswith("#") or "=" not in linea:
            continue
        clave, valor = linea.split("=", 1)
        clave = clave.strip()
        if clave not in CLAVES_PERMITIDAS or clave in os.environ:
            continue
        os.environ[clave] = valor.strip().strip('"').strip("'")


def cargar_entornos() -> None:
    raiz = Path(__file__).resolve().parents[1]
    cargar_env(raiz / ".env")
    cwd = Path.cwd() / ".env"
    if cwd.resolve() != (raiz / ".env").resolve():
        cargar_env(cwd)


def configuracion_publica() -> dict:
    """Datos para rellenar el formulario. Nunca incluye la clave."""
    return {
        "usuario": os.environ.get("SMTP_USUARIO", ""),
        "servidor": os.environ.get("SMTP_SERVIDOR", ""),
        "puerto": os.environ.get("SMTP_PUERTO", ""),
        "remitente": os.environ.get("SMTP_REMITENTE", ""),
        "nombre": os.environ.get("SMTP_NOMBRE", ""),
        "cifrado": os.environ.get("SMTP_CIFRADO", ""),
        "clave_disponible": bool(os.environ.get("SMTP_CLAVE")),
    }


def clave_del_entorno() -> str:
    return os.environ.get("SMTP_CLAVE", "")
