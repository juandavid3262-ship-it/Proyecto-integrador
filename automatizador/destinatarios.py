"""Lectura de listas de destinatarios: un correo por persona."""

from __future__ import annotations

import csv
import io
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

ENCABEZADOS_CORREO = {
    "email",
    "e-mail",
    "mail",
    "correo",
    "correo electronico",
}
ENCABEZADOS_NOMBRE = {
    "nombre",
    "nombres",
    "name",
    "destinatario",
}


@dataclass(frozen=True)
class Destinatario:
    email: str
    nombre: str = ""

    @property
    def clave(self) -> str:
        return self.email.casefold()


@dataclass(frozen=True)
class ListaDestinatarios:
    validos: list[Destinatario]
    invalidos: list[str]
    duplicados: list[str]


def es_email(valor: str) -> bool:
    if not valor or len(valor) > 254 or valor.count("@") != 1:
        return False
    if any(caracter in valor for caracter in " \t\r\n\x00"):
        return False
    local, dominio = valor.split("@")
    if not local or not dominio or len(local) > 64:
        return False
    if local.startswith(".") or local.endswith(".") or ".." in local:
        return False
    if ".." in dominio or dominio.startswith(".") or dominio.endswith("."):
        return False
    if not re.fullmatch(r"[A-Za-z0-9._%+\-]+", local):
        return False
    if not re.fullmatch(r"[A-Za-z0-9.\-]+\.[A-Za-z]{2,63}", dominio):
        return False
    for etiqueta in dominio.split("."):
        if not etiqueta or etiqueta.startswith("-") or etiqueta.endswith("-"):
            return False
    return True


def leer_archivo(ruta: Path) -> str:
    crudo = ruta.read_bytes()
    for codificacion in ("utf-8-sig", "utf-8", "cp1252"):
        try:
            return crudo.decode(codificacion)
        except UnicodeDecodeError:
            continue
    return crudo.decode("utf-8", errors="replace")


def parse_destinatarios(texto: str) -> ListaDestinatarios:
    limpio = texto.replace("\ufeff", "").replace("\x00", "").strip()
    if not limpio:
        return ListaDestinatarios([], [], [])
    primera = next((linea for linea in limpio.splitlines() if linea.strip()), "")
    if _es_encabezado(primera):
        return _parse_csv(limpio, _delimitador(primera))
    return _parse_lineas(limpio)


def _norm(valor: str) -> str:
    base = unicodedata.normalize("NFD", valor.strip().casefold())
    sin_tilde = "".join(c for c in base if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", sin_tilde)


def _delimitador(linea: str) -> str:
    conteos = {",": linea.count(","), ";": linea.count(";"), "\t": linea.count("\t")}
    return max(conteos, key=conteos.get)


def _es_encabezado(linea: str) -> bool:
    if not any(separador in linea for separador in ",;\t"):
        return False
    celdas = next(csv.reader([linea], delimiter=_delimitador(linea)))
    return any(_norm(celda) in ENCABEZADOS_CORREO for celda in celdas)


def _parse_csv(texto: str, delimitador: str) -> ListaDestinatarios:
    lector = csv.DictReader(io.StringIO(texto), delimiter=delimitador)
    if not lector.fieldnames:
        return ListaDestinatarios([], [], [])
    columnas = {_norm(nombre): nombre for nombre in lector.fieldnames if nombre}
    columna_correo = next((columnas[n] for n in ENCABEZADOS_CORREO if n in columnas), None)
    columna_nombre = next((columnas[n] for n in ENCABEZADOS_NOMBRE if n in columnas), None)
    if columna_correo is None:
        return _parse_lineas(texto)
    candidatos: list[tuple[str, str]] = []
    invalidos: list[str] = []
    for fila in lector:
        crudo = (fila.get(columna_correo) or "").strip()
        nombre = (fila.get(columna_nombre) or "").strip() if columna_nombre else ""
        if not crudo and not nombre:
            continue
        if es_email(crudo):
            candidatos.append((crudo, nombre))
        else:
            visible = ", ".join(parte.strip() for parte in fila.values() if parte and parte.strip())
            invalidos.append(visible or crudo or "(fila vacía)")
    return _deduplicar(candidatos, invalidos)


def _parse_lineas(texto: str) -> ListaDestinatarios:
    candidatos: list[tuple[str, str]] = []
    invalidos: list[str] = []
    for linea in texto.splitlines():
        original = linea.strip()
        if not original or original.startswith("#"):
            continue
        leido = _parse_linea(original)
        if leido is None:
            invalidos.append(original)
        else:
            candidatos.append(leido)
    return _deduplicar(candidatos, invalidos)


def _parse_linea(linea: str) -> tuple[str, str] | None:
    con_angulo = re.fullmatch(r"\s*(.*?)\s*<\s*([^<>\s]+)\s*>\s*", linea)
    if con_angulo:
        nombre = con_angulo.group(1).strip().strip("\"'")
        correo = con_angulo.group(2).strip()
        if es_email(correo):
            return correo, nombre
        return None
    if any(separador in linea for separador in ",;\t"):
        partes = re.split(r"[,;\t]", linea, maxsplit=1)
        if len(partes) == 2:
            izquierda, derecha = partes[0].strip().strip("\"'"), partes[1].strip().strip("\"'")
            if es_email(izquierda) and not es_email(derecha):
                return izquierda, derecha
            if es_email(derecha) and not es_email(izquierda):
                return derecha, izquierda
            return None
    if es_email(linea.strip()):
        return linea.strip(), ""
    return None


def _deduplicar(candidatos: list[tuple[str, str]], invalidos: list[str]) -> ListaDestinatarios:
    vistos: set[str] = set()
    validos: list[Destinatario] = []
    duplicados: list[str] = []
    for correo, nombre in candidatos:
        clave = correo.casefold()
        if clave in vistos:
            duplicados.append(correo)
            continue
        vistos.add(clave)
        validos.append(Destinatario(correo, _una_celda(nombre)))
    return ListaDestinatarios(validos, invalidos, duplicados)


def _una_celda(valor: str) -> str:
    return re.sub(r"[\r\n\x00]+", " ", valor).strip()
