import json
import os
import sys
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from automatizador.correo import ErrorTransitorio, TransporteSimulado
from automatizador.servidor import crear_servidor


SECRETO = "super-secreta-xyz"


def lista_de(cantidad):
    return "\n".join(f"persona{i}@ejemplo.com" for i in range(cantidad))


class PruebasServidor(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.httpd = crear_servidor(Path(self.tmp.name), puerto=0, fabrica=self.fabrica)
        self.puerto = self.httpd.server_address[1]
        self.hilo = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.hilo.start()
        self.addCleanup(self._apagar)
        self.transportes = []
        self.fabrica_lenta = None
        self.empezado = threading.Event()
        self.seguir = threading.Event()

    def _apagar(self):
        self.seguir.set()
        self.httpd.shutdown()
        self.httpd.server_close()
        self.hilo.join(timeout=5)

    def fabrica(self, _cuenta, simular):
        if self.fabrica_lenta is not None:
            return self.fabrica_lenta()
        transporte = TransporteSimulado()
        self.transportes.append((simular, transporte))
        return transporte

    def pedir(self, metodo, ruta, cuerpo=None):
        datos = None if cuerpo is None else json.dumps(cuerpo).encode()
        headers = {"Content-Type": "application/json"} if datos is not None else {}
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.puerto}{ruta}",
            data=datos,
            method=metodo,
            headers=headers,
        )
        try:
            with urllib.request.urlopen(req, timeout=5) as resp:
                crudo = resp.read()
                return resp.status, json.loads(crudo.decode()) if crudo else {}
        except urllib.error.HTTPError as exc:
            crudo = exc.read()
            return exc.code, json.loads(crudo.decode()) if crudo else {}

    def test_pagina_y_rutas_fuera_del_directorio(self):
        req = urllib.request.Request(f"http://127.0.0.1:{self.puerto}/")
        with urllib.request.urlopen(req, timeout=5) as resp:
            html = resp.read().decode()
        self.assertIn("Enviar correos, uno por uno", html)
        self.assertIn("boton-enviar", html)
        codigo, _ = self.pedir("GET", "/%2e%2e/%2e%2e/etc/passwd")
        self.assertEqual(codigo, 404)

    def test_revisar_408_identicos(self):
        codigo, datos = self.pedir(
            "POST",
            "/api/revisar",
            {
                "destinatarios": lista_de(408),
                "asunto": "Aviso",
                "texto": "El mismo texto para las 408 personas.",
            },
        )
        self.assertEqual(codigo, 200)
        self.assertEqual(datos["total"], 408)
        self.assertTrue(datos["identico"])
        self.assertEqual(datos["muestra"][0]["email"], "persona0@ejemplo.com")
        self.assertEqual(datos["cierre"][-1]["email"], "persona407@ejemplo.com")
        self.assertNotIn(SECRETO, json.dumps(datos))

    def test_simular_408_uno_por_uno(self):
        codigo, _ = self.pedir(
            "POST",
            "/api/simular",
            {"destinatarios": lista_de(408), "asunto": "Aviso", "texto": "Mismo contenido", "pausa": 0, "clave": SECRETO},
        )
        self.assertEqual(codigo, 200)
        estado = self._esperar_fin()
        self.assertEqual(estado["fase"], "done")
        self.assertEqual(estado["simulados"], 408)
        self.assertEqual(len({fila["email"] for fila in estado["resultados"]}), 408)
        self.assertNotIn(SECRETO, json.dumps(estado))
        self.assertEqual(self.transportes[0][0], True)
        self.assertEqual(len(self.transportes[0][1].mensajes), 408)

    def test_enviar_exige_el_numero_y_no_guarda_la_clave(self):
        base = {
            "destinatarios": lista_de(2),
            "asunto": "Aviso",
            "texto": "Mismo contenido",
            "host": "smtp.ejemplo.com",
            "puerto": 587,
            "usuario": "remitente@ejemplo.com",
            "remitente": "remitente@ejemplo.com",
            "clave": SECRETO,
            "pausa": 0,
        }
        codigo, datos = self.pedir("POST", "/api/enviar", {**base, "confirmar": 1})
        self.assertEqual(codigo, 400)
        self.assertIn("2", datos["mensaje"])
        codigo, _ = self.pedir("POST", "/api/enviar", {**base, "confirmar": 2})
        self.assertEqual(codigo, 200)
        estado = self._esperar_fin()
        self.assertEqual(estado["enviados"], 2)
        self.assertEqual(len(self.transportes[-1][1].mensajes), 2)
        self.assertFalse(self.transportes[-1][0])
        bitacora = "\n".join(
            ruta.read_text(encoding="utf-8")
            for ruta in Path(self.tmp.name).rglob("*")
            if ruta.is_file()
        )
        self.assertNotIn(SECRETO, bitacora)
        self.assertNotIn(SECRETO, json.dumps(estado))

    def test_no_hay_dos_envios_a_la_vez(self):
        class Lento(TransporteSimulado):
            def __init__(self, prueba):
                super().__init__()
                self.prueba = prueba

            def enviar(self, mensaje):
                self.prueba.empezado.set()
                if not self.prueba.seguir.wait(timeout=5):
                    raise ErrorTransitorio("tiempo")
                super().enviar(mensaje)

        self.fabrica_lenta = lambda: Lento(self)
        codigo, _ = self.pedir(
            "POST",
            "/api/simular",
            {"destinatarios": "ana@ejemplo.com", "asunto": "Aviso", "texto": "Hola", "pausa": 0},
        )
        self.assertEqual(codigo, 200)
        self.assertTrue(self.empezado.wait(timeout=5))
        codigo, datos = self.pedir(
            "POST",
            "/api/simular",
            {"destinatarios": "luis@ejemplo.com", "asunto": "Aviso", "texto": "Hola", "pausa": 0},
        )
        self.assertEqual(codigo, 409)
        self.assertIn("curso", datos["mensaje"])
        self.seguir.set()
        estado = self._esperar_fin()
        self.assertEqual(estado["simulados"], 1)

    def test_configuracion_no_revela_la_clave(self):
        anterior = os.environ.get("SMTP_CLAVE")
        os.environ["SMTP_CLAVE"] = SECRETO
        os.environ["SMTP_USUARIO"] = "ada@ejemplo.com"
        try:
            codigo, datos = self.pedir("GET", "/api/configuracion")
        finally:
            if anterior is None:
                os.environ.pop("SMTP_CLAVE", None)
            else:
                os.environ["SMTP_CLAVE"] = anterior
            os.environ.pop("SMTP_USUARIO", None)
        self.assertEqual(codigo, 200)
        self.assertTrue(datos["clave_disponible"])
        self.assertEqual(datos["usuario"], "ada@ejemplo.com")
        self.assertNotIn(SECRETO, json.dumps(datos))
        self.assertNotIn("clave", datos)

    def test_detener(self):
        class Lento(TransporteSimulado):
            def __init__(self, prueba):
                super().__init__()
                self.prueba = prueba

            def enviar(self, mensaje):
                self.prueba.empezado.set()
                self.prueba.seguir.wait(timeout=5)
                super().enviar(mensaje)

        self.fabrica_lenta = lambda: Lento(self)
        self.pedir(
            "POST",
            "/api/simular",
            {"destinatarios": lista_de(3), "asunto": "Aviso", "texto": "Hola", "pausa": 0},
        )
        self.assertTrue(self.empezado.wait(timeout=5))
        codigo, _ = self.pedir("POST", "/api/detener", {})
        self.assertEqual(codigo, 200)
        self.seguir.set()
        estado = self._esperar_fin()
        self.assertEqual(estado["fase"], "stopped")
        self.assertLess(estado["procesados"], 3)

    def _esperar_fin(self):
        estado = {}
        for _ in range(100):
            _codigo, estado = self.pedir("GET", "/api/estado")
            if estado.get("fase") in {"done", "error", "stopped"}:
                return estado
            time.sleep(0.02)
        self.fail(f"el envío no terminó: {estado}")


class PruebasCli(unittest.TestCase):
    def test_simular_el_ejemplo(self):
        import subprocess

        raiz = Path(__file__).resolve().parents[1]
        resultado = subprocess.run(
            [
                sys.executable,
                "-m",
                "automatizador.cli",
                "--destinatarios",
                str(raiz / "ejemplo" / "destinatarios.csv"),
                "--mensaje",
                str(raiz / "ejemplo" / "mensaje.txt"),
                "--asunto",
                "Aviso de prueba",
                "--simular",
                "--pausa",
                "0",
            ],
            cwd=raiz,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(resultado.returncode, 0, resultado.stderr)
        self.assertIn("idéntico", resultado.stdout)
        self.assertIn("Simulación lista: 3", resultado.stdout)

    def test_enviar_sin_confirmacion_no_sale(self):
        import subprocess

        raiz = Path(__file__).resolve().parents[1]
        entorno = os.environ.copy()
        entorno.pop("SMTP_CLAVE", None)
        entorno["CONFIRMAR_CANTIDAD"] = "1"
        resultado = subprocess.run(
            [
                sys.executable,
                "-m",
                "automatizador.cli",
                "--destinatarios",
                str(raiz / "ejemplo" / "destinatarios.csv"),
                "--mensaje",
                str(raiz / "ejemplo" / "mensaje.txt"),
                "--asunto",
                "Aviso",
                "--enviar",
                "--usuario",
                "ada@ejemplo.com",
            ],
            cwd=raiz,
            capture_output=True,
            text=True,
            check=False,
            env=entorno,
        )
        self.assertEqual(resultado.returncode, 2, resultado.stderr)
        self.assertIn("No se envió nada", resultado.stdout)


if __name__ == "__main__":
    unittest.main()
