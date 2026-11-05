"""Arma un mensaje por destinatario y lo entrega por SMTP."""

from __future__ import annotations

import re
import smtplib
from dataclasses import dataclass, replace
from datetime import datetime
from email.message import EmailMessage
from email.utils import format_datetime, formataddr, make_msgid
from zoneinfo import ZoneInfo

from automatizador.destinatarios import Destinatario, es_email

ZONA = ZoneInfo("America/Bogota")
LIMITE_DESTINATARIOS = 5000
REINTENTOS = 3


class ErrorCorreo(Exception):
    """Fallo al armar o entregar un correo."""


class ErrorAutenticacion(ErrorCorreo):
    """La cuenta o el remitente fueron rechazados. Detiene el lote."""


class ErrorEnvio(ErrorCorreo):
    """Fallo permanente de un solo destinatario."""


class ErrorTransitorio(ErrorCorreo):
    """Fallo temporal. El lote puede reintentar."""


@dataclass(frozen=True)
class CuentaSmtp:
    host: str
    port: int
    usuario: str
    clave: str
    remitente: str
    nombre_remitente: str = ""
    cifrado: str = "starttls"

    def validar(self) -> list[str]:
        errores: list[str] = []
        if not self.host.strip():
            errores.append("Falta el servidor SMTP.")
        if not 1 <= self.port <= 65535:
            errores.append("El puerto SMTP no es válido.")
        if self.cifrado not in {"starttls", "ssl", "ninguno"}:
            errores.append("El cifrado debe ser starttls, ssl o ninguno.")
        if not self.usuario.strip():
            errores.append("Falta el usuario SMTP.")
        if not self.clave:
            errores.append("Falta la clave SMTP.")
        if not es_email(self.remitente):
            errores.append("El remitente no es un correo válido.")
        return errores


@dataclass(frozen=True)
class Mensaje:
    asunto: str
    texto: str
    html: str = ""

    def validar(self) -> list[str]:
        errores: list[str] = []
        if not self.asunto.strip():
            errores.append("Falta el asunto.")
        elif len(self.asunto) > 300:
            errores.append("El asunto es demasiado largo.")
        if not self.texto.strip() and not self.html.strip():
            errores.append("Falta el cuerpo del correo.")
        if len(self.texto) > 200_000 or len(self.html) > 500_000:
            errores.append("El cuerpo del correo es demasiado largo.")
        return errores


def una_linea(valor: str) -> str:
    return re.sub(r"[\r\n\x00]+", " ", valor).strip()


def personalizar(plantilla: str, dest: Destinatario, indice: int, total: int) -> str:
    nombre = dest.nombre.strip() or dest.email.split("@", 1)[0]
    valores = {
        "{{nombre}}": nombre,
        "{{email}}": dest.email,
        "{{n}}": str(indice),
        "{{total}}": str(total),
    }
    partes = re.split(r"(\{\{(?:nombre|email|n|total)\}\})", plantilla)
    return "".join(valores.get(parte, parte) for parte in partes)


def renderizar(dest: Destinatario, mensaje: Mensaje, indice: int, total: int) -> dict[str, str]:
    texto = personalizar(mensaje.texto, dest, indice, total)
    html = personalizar(mensaje.html, dest, indice, total) if mensaje.html.strip() else ""
    if not texto.strip():
        texto = "Abre este correo con un lector compatible con HTML."
    return {
        "asunto": una_linea(personalizar(mensaje.asunto, dest, indice, total)),
        "texto": texto,
        "html": html,
        "nombre_destinatario": una_linea(dest.nombre),
    }


def construir_vista(destinatarios: list[Destinatario], mensaje: Mensaje) -> dict:
    primera: dict | None = None
    ultima: dict | None = None
    firmas: set[tuple[str, str, str]] = set()
    total = len(destinatarios)
    for indice, dest in enumerate(destinatarios, start=1):
        render = renderizar(dest, mensaje, indice, total)
        firmas.add((render["asunto"], render["texto"], render["html"]))
        ficha = {
            "email": dest.email,
            "nombre": dest.nombre,
            "asunto": render["asunto"],
            "texto": render["texto"],
        }
        if primera is None:
            primera = ficha
        ultima = ficha
    identico = len(firmas) <= 1
    muestras: list[dict] = []
    if primera is not None:
        muestras.append(primera)
        if not identico and ultima is not None and ultima["email"] != primera["email"]:
            muestras.append(ultima)
    return {"total": total, "identico": identico, "muestras_mensaje": muestras}


def construir_mensaje(
    cuenta: CuentaSmtp,
    mensaje: Mensaje,
    dest: Destinatario,
    indice: int,
    total: int,
) -> EmailMessage:
    render = renderizar(dest, mensaje, indice, total)
    correo = EmailMessage()
    correo["Subject"] = render["asunto"]
    nombre_remitente = una_linea(cuenta.nombre_remitente)
    if nombre_remitente:
        correo["From"] = formataddr((nombre_remitente, cuenta.remitente))
    else:
        correo["From"] = cuenta.remitente
    if render["nombre_destinatario"]:
        correo["To"] = formataddr((render["nombre_destinatario"], dest.email))
    else:
        correo["To"] = dest.email
    correo["Date"] = format_datetime(datetime.now(ZONA))
    correo["Message-ID"] = make_msgid()
    correo.set_content(render["texto"], charset="utf-8")
    if render["html"]:
        correo.add_alternative(render["html"], subtype="html", charset="utf-8")
    if correo["Cc"] is not None or correo["Bcc"] is not None:
        raise ErrorCorreo("Cada envío debe ir sin copias.")
    if len(list(correo["To"].addresses)) != 1:
        raise ErrorCorreo("Cada envío debe tener un solo destinatario.")
    if correo["To"].addresses[0].addr_spec.casefold() != dest.email.casefold():
        raise ErrorCorreo("El destinatario del sobre no coincide con la lista.")
    return correo


def cuenta_de_envio(cuenta: CuentaSmtp, simular: bool) -> CuentaSmtp:
    if es_email(cuenta.remitente):
        return cuenta
    if simular:
        return replace(cuenta, remitente="simulacion@local.test")
    return cuenta


class TransporteSmtp:
    def __init__(self, cuenta: CuentaSmtp, timeout: float = 40):
        self.cuenta = cuenta
        self.timeout = timeout
        self._smtp: smtplib.SMTP | None = None

    def conectar(self) -> None:
        self.cerrar()
        cuenta = self.cuenta
        smtp: smtplib.SMTP | None = None
        try:
            if cuenta.cifrado == "ssl":
                smtp = smtplib.SMTP_SSL(cuenta.host, cuenta.port, timeout=self.timeout)
                smtp.ehlo()
            else:
                smtp = smtplib.SMTP(cuenta.host, cuenta.port, timeout=self.timeout)
                smtp.ehlo()
                if cuenta.cifrado == "starttls":
                    smtp.starttls()
                    smtp.ehlo()
            if cuenta.usuario:
                smtp.login(cuenta.usuario, cuenta.clave)
        except smtplib.SMTPAuthenticationError as exc:
            _cerrar_smtp(smtp)
            raise ErrorAutenticacion("El servidor rechazó el usuario o la clave.") from exc
        except (OSError, smtplib.SMTPException) as exc:
            _cerrar_smtp(smtp)
            raise ErrorTransitorio("No se pudo conectar al servidor de correo.") from exc
        self._smtp = smtp

    def enviar(self, mensaje: EmailMessage) -> None:
        if self._smtp is None:
            self.conectar()
        try:
            self._enviar_una_vez(mensaje)
        except smtplib.SMTPServerDisconnected:
            self.cerrar()
            self.conectar()
            try:
                self._enviar_una_vez(mensaje)
            except smtplib.SMTPServerDisconnected as exc:
                raise ErrorTransitorio("El servidor cerró la conexión.") from exc

    def _enviar_una_vez(self, mensaje: EmailMessage) -> None:
        assert self._smtp is not None
        try:
            self._smtp.send_message(mensaje)
        except smtplib.SMTPServerDisconnected:
            raise
        except smtplib.SMTPRecipientsRefused as exc:
            raise ErrorEnvio("El servidor rechazó ese destinatario.") from exc
        except smtplib.SMTPSenderRefused as exc:
            raise ErrorAutenticacion("El servidor rechazó el remitente.") from exc
        except smtplib.SMTPResponseException as exc:
            if exc.smtp_code in {530, 534, 535, 538}:
                raise ErrorAutenticacion("El servidor rechazó la sesión.") from exc
            if 400 <= exc.smtp_code < 500:
                raise ErrorTransitorio("El servidor pidió reintentar más tarde.") from exc
            raise ErrorEnvio("El servidor rechazó el mensaje.") from exc
        except (TimeoutError, OSError) as exc:
            self.cerrar()
            raise ErrorTransitorio("Falló la red durante el envío.") from exc

    def cerrar(self) -> None:
        smtp = self._smtp
        self._smtp = None
        if smtp is None:
            return
        try:
            smtp.quit()
        except (OSError, smtplib.SMTPException):
            try:
                smtp.close()
            except (OSError, smtplib.SMTPException):
                pass


def _cerrar_smtp(smtp: smtplib.SMTP | None) -> None:
    if smtp is None:
        return
    try:
        smtp.quit()
    except (OSError, smtplib.SMTPException):
        try:
            smtp.close()
        except (OSError, smtplib.SMTPException):
            pass


class TransporteSimulado:
    """No abre ninguna conexión. Sirve para revisar el lote antes de enviarlo."""

    def __init__(self) -> None:
        self.mensajes: list[EmailMessage] = []
        self.conectado = False

    def conectar(self) -> None:
        self.conectado = True

    def enviar(self, mensaje: EmailMessage) -> None:
        if not self.conectado:
            raise ErrorTransitorio("La simulación no está conectada.")
        if mensaje["Cc"] is not None or mensaje["Bcc"] is not None:
            raise ErrorEnvio("No se permiten copias.")
        if len(list(mensaje["To"].addresses)) != 1:
            raise ErrorEnvio("Se esperaba un solo destinatario.")
        self.mensajes.append(mensaje)

    def cerrar(self) -> None:
        self.conectado = False
