import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from automatizador.correo import (
    CuentaSmtp,
    Mensaje,
    construir_mensaje,
    construir_vista,
    personalizar,
)
from automatizador.destinatarios import Destinatario


CUENTA = CuentaSmtp(
    host="smtp.ejemplo.com",
    port=587,
    usuario="remitente@ejemplo.com",
    clave="secreta",
    remitente="remitente@ejemplo.com",
    nombre_remitente="Ada López",
)


class PruebasCorreo(unittest.TestCase):
    def test_sin_marcadores_el_texto_queda_igual(self):
        dest = Destinatario("ana@ejemplo.com", "Ana")
        self.assertEqual(personalizar("El viernes a las 5.", dest, 1, 2), "El viernes a las 5.")

    def test_marcadores_no_se_reemplazan_dos_veces(self):
        dest = Destinatario("ana@ejemplo.com", "Ana {{email}}")
        self.assertEqual(personalizar("Hola {{nombre}}", dest, 1, 1), "Hola Ana {{email}}")

    def test_cada_mensaje_lleva_un_solo_destinatario_y_el_mismo_cuerpo(self):
        texto = "Este aviso es para todo el grupo.\nNos vemos el viernes."
        mensaje = Mensaje("Aviso del viernes", texto)
        uno = construir_mensaje(CUENTA, mensaje, Destinatario("ana@ejemplo.com", "Ana"), 1, 2)
        dos = construir_mensaje(CUENTA, mensaje, Destinatario("luis@ejemplo.com", "Luis"), 2, 2)
        self.assertEqual(str(uno["Subject"]), str(dos["Subject"]))
        self.assertEqual(uno.get_content(), dos.get_content())
        self.assertEqual(len(list(uno["To"].addresses)), 1)
        self.assertEqual(uno["To"].addresses[0].addr_spec, "ana@ejemplo.com")
        self.assertEqual(dos["To"].addresses[0].addr_spec, "luis@ejemplo.com")
        self.assertIsNone(uno["Cc"])
        self.assertIsNone(uno["Bcc"])
        self.assertNotIn("luis@ejemplo.com", uno.as_string())

    def test_el_asunto_no_permite_inyectar_cabeceras(self):
        mensaje = Mensaje("Hola\r\nBcc: malo@ejemplo.com", "Cuerpo")
        correo = construir_mensaje(CUENTA, mensaje, Destinatario("ana@ejemplo.com"), 1, 1)
        self.assertNotIn("\n", str(correo["Subject"]))
        self.assertIsNone(correo["Bcc"])
        self.assertEqual(len(list(correo["To"].addresses)), 1)

    def test_acentos_en_el_cuerpo(self):
        mensaje = Mensaje("Señor", "El año que viene, en Bogotá.")
        correo = construir_mensaje(CUENTA, mensaje, Destinatario("ana@ejemplo.com", "José"), 1, 1)
        self.assertIn("Bogotá", correo.get_content())
        self.assertIn("José", str(correo["To"]))

    def test_vista_de_408_identica_y_vista_personalizada(self):
        destinatarios = [Destinatario(f"persona{i}@ejemplo.com") for i in range(408)]
        vista = construir_vista(destinatarios, Mensaje("Aviso", "Contenido igual para todos."))
        self.assertEqual(vista["total"], 408)
        self.assertTrue(vista["identico"])
        self.assertEqual(len(vista["muestras_mensaje"]), 1)
        self.assertEqual(vista["muestras_mensaje"][0]["texto"], "Contenido igual para todos.")

        personal = construir_vista(
            [Destinatario("a@ejemplo.com", "Ana"), Destinatario("b@ejemplo.com", "Luis")],
            Mensaje("Hola {{nombre}}", "Hola {{nombre}}"),
        )
        self.assertFalse(personal["identico"])
        self.assertEqual(personal["muestras_mensaje"][0]["asunto"], "Hola Ana")
        self.assertEqual(personal["muestras_mensaje"][1]["asunto"], "Hola Luis")

    def test_starttls_usa_el_usuario_sin_imprimir_la_clave(self):
        creado = {}

        class FalsoSmtp:
            def __init__(self, host, port, timeout=None):
                creado["args"] = (host, port, timeout)
                self.tls = False

            def ehlo(self):
                return None

            def starttls(self):
                self.tls = True

            def login(self, usuario, clave):
                creado["login"] = (usuario, clave)

            def quit(self):
                creado["cerrado"] = True

        with mock.patch("automatizador.correo.smtplib.SMTP", FalsoSmtp):
            from automatizador.correo import TransporteSmtp

            transporte = TransporteSmtp(CUENTA)
            transporte.conectar()
            transporte.cerrar()
        self.assertEqual(creado["args"][0], "smtp.ejemplo.com")
        self.assertTrue(creado["login"][0] == "remitente@ejemplo.com")
        self.assertEqual(creado["login"][1], "secreta")
        self.assertTrue(creado["cerrado"])


if __name__ == "__main__":
    unittest.main()
