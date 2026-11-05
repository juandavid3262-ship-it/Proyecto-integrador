import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from automatizador.campana import Campana, correos_enviados, vista_en_disco
from automatizador.correo import (
    CuentaSmtp,
    ErrorAutenticacion,
    ErrorEnvio,
    ErrorTransitorio,
    Mensaje,
    TransporteSimulado,
)
from automatizador.destinatarios import Destinatario


CUENTA = CuentaSmtp(
    host="smtp.ejemplo.com",
    port=587,
    usuario="remitente@ejemplo.com",
    clave="super-secreta-xyz",
    remitente="remitente@ejemplo.com",
    nombre_remitente="Ada",
)
TEXTO = "Este es el aviso para todo el grupo.\nNos vemos el viernes."
MENSAJE = Mensaje("Aviso del viernes", TEXTO)


def personas(cantidad):
    return [Destinatario(f"persona{i}@ejemplo.com", f"Persona {i}") for i in range(cantidad)]


class PruebasCampana(unittest.TestCase):
    def test_408_individuales_con_el_mismo_contenido(self):
        transporte = TransporteSimulado()
        campana = Campana(personas(408), MENSAJE, CUENTA, transporte, pausa=0, simular=True)
        campana.ejecutar()
        self.assertEqual(campana.fase, "done")
        self.assertEqual(campana.simulados, 408)
        self.assertEqual(len(transporte.mensajes), 408)
        cuerpos = set()
        asuntos = set()
        destinos = []
        for correo in transporte.mensajes:
            self.assertIsNone(correo["Cc"])
            self.assertIsNone(correo["Bcc"])
            self.assertEqual(len(list(correo["To"].addresses)), 1)
            cuerpos.add(correo.get_content())
            asuntos.add(str(correo["Subject"]))
            destinos.append(correo["To"].addresses[0].addr_spec)
        self.assertEqual(len(cuerpos), 1)
        self.assertIn(TEXTO, next(iter(cuerpos)))
        self.assertEqual(asuntos, {"Aviso del viernes"})
        self.assertEqual(len(set(destinos)), 408)
        self.assertNotIn("persona1@ejemplo.com", transporte.mensajes[0].as_string())
        self.assertNotIn("super-secreta-xyz", transporte.mensajes[0].as_string())

    def test_pausa_entre_envios_y_no_despues_del_ultimo(self):
        pausas = []
        campana = Campana(
            personas(3),
            MENSAJE,
            CUENTA,
            TransporteSimulado(),
            pausa=2,
            simular=True,
            dormir=pausas.append,
        )
        campana.ejecutar()
        self.assertEqual(pausas, [2, 2])

    def test_un_rechazo_no_detiene_al_resto(self):
        class Selectivo(TransporteSimulado):
            def enviar(self, mensaje):
                if mensaje["To"].addresses[0].addr_spec == "persona1@ejemplo.com":
                    raise ErrorEnvio("El servidor rechazó ese destinatario.")
                super().enviar(mensaje)

        transporte = Selectivo()
        campana = Campana(personas(3), MENSAJE, CUENTA, transporte, pausa=0, simular=False)
        campana.ejecutar()
        self.assertEqual(campana.fase, "done")
        self.assertEqual(campana.enviados, 2)
        self.assertEqual(campana.fallidos, 1)
        self.assertEqual(len(transporte.mensajes), 2)

    def test_fallo_de_autenticacion_no_sigue_con_la_lista(self):
        class Auth:
            def conectar(self):
                raise ErrorAutenticacion("El servidor rechazó el usuario o la clave.")

            def enviar(self, _mensaje):
                raise AssertionError("no debía enviar")

            def cerrar(self):
                return None

        campana = Campana(personas(5), MENSAJE, CUENTA, Auth(), pausa=0)
        campana.ejecutar()
        self.assertEqual(campana.fase, "error")
        self.assertEqual(campana.enviados, 0)
        self.assertIn("rechazó", campana.mensaje_estado)

    def test_reintenta_un_corte_y_luego_sigue(self):
        class Inestable(TransporteSimulado):
            def __init__(self):
                super().__init__()
                self.intentos = 0

            def enviar(self, mensaje):
                self.intentos += 1
                if self.intentos < 3:
                    raise ErrorTransitorio("corte")
                super().enviar(mensaje)

        esperas = []
        transporte = Inestable()
        campana = Campana(
            personas(1),
            MENSAJE,
            CUENTA,
            transporte,
            pausa=0,
            espera_reintento=0,
            dormir=esperas.append,
        )
        campana.ejecutar()
        self.assertEqual(campana.enviados, 1)
        self.assertEqual(transporte.intentos, 3)
        self.assertEqual(esperas, [0, 0])

    def test_detener_no_envia_el_siguiente(self):
        class Detiene(TransporteSimulado):
            def __init__(self, campana):
                super().__init__()
                self.campana = campana

            def enviar(self, mensaje):
                super().enviar(mensaje)
                self.campana.detener.set()

        campana = Campana(personas(4), MENSAJE, CUENTA, TransporteSimulado(), pausa=9)
        campana.transporte = Detiene(campana)
        campana.ejecutar()
        self.assertEqual(campana.fase, "stopped")
        self.assertEqual(len(campana.transporte.mensajes), 1)

    def test_reanudar_salta_los_ya_enviados(self):
        directorio = Path(tempfile.mkdtemp())
        self.addCleanup(lambda: None)
        transporte = TransporteSimulado()
        primeros = Campana(
            personas(2),
            MENSAJE,
            CUENTA,
            transporte,
            pausa=0,
            ruta_jsonl=directorio / "ultimo.jsonl",
            ruta_meta=directorio / "ultimo.meta.json",
        )
        primeros.ejecutar()
        self.assertEqual(correos_enviados(directorio / "ultimo.jsonl"), {
            "persona0@ejemplo.com",
            "persona1@ejemplo.com",
        })
        segundo_transporte = TransporteSimulado()
        segundo = Campana(
            personas(3),
            MENSAJE,
            CUENTA,
            segundo_transporte,
            pausa=0,
            ya_enviados=correos_enviados(directorio / "ultimo.jsonl"),
            ruta_jsonl=directorio / "ultimo.jsonl",
            ruta_meta=directorio / "ultimo.meta.json",
        )
        segundo.ejecutar()
        self.assertEqual(len(segundo_transporte.mensajes), 1)
        self.assertEqual(segundo.omitidos, 2)
        self.assertEqual(segundo.enviados, 1)
        self.assertNotIn("super-secreta-xyz", (directorio / "ultimo.jsonl").read_text(encoding="utf-8"))
        vista = vista_en_disco(directorio)
        self.assertEqual(vista["fase"], "done")
        self.assertGreaterEqual(vista["enviados"], 3)


if __name__ == "__main__":
    unittest.main()
