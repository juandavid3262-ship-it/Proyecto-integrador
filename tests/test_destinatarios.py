import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from automatizador.destinatarios import es_email, leer_archivo, parse_destinatarios


class PruebasDestinatarios(unittest.TestCase):
    def test_lineas_simples_y_angulos(self):
        texto = "\n".join(
            [
                "ana@ejemplo.com",
                "Luis Pérez <luis@ejemplo.com>",
                "Gómez, Ana <ana.gomez@ejemplo.com>",
                "",
                "# comentario",
                "marta@ejemplo.com, Marta Ríos",
            ]
        )
        lista = parse_destinatarios(texto)
        self.assertEqual(
            [d.email for d in lista.validos],
            ["ana@ejemplo.com", "luis@ejemplo.com", "ana.gomez@ejemplo.com", "marta@ejemplo.com"],
        )
        self.assertEqual(lista.validos[1].nombre, "Luis Pérez")
        self.assertEqual(lista.validos[2].nombre, "Gómez, Ana")
        self.assertEqual(lista.validos[3].nombre, "Marta Ríos")

    def test_csv_con_punto_y_coma_y_tilde_en_encabezado(self):
        texto = "Nombre;Correo electrónico\nAna Gómez;ana@ejemplo.com\n"
        lista = parse_destinatarios(texto)
        self.assertEqual(len(lista.validos), 1)
        self.assertEqual(lista.validos[0].email, "ana@ejemplo.com")
        self.assertEqual(lista.validos[0].nombre, "Ana Gómez")

    def test_duplicados_e_invalidos(self):
        texto = "\n".join(
            [
                "Ana@ejemplo.com",
                "ana@ejemplo.com",
                "no-es-correo",
                "a@x.com, b@x.com",
                "bien@ejemplo.com",
            ]
        )
        lista = parse_destinatarios(texto)
        self.assertEqual([d.email for d in lista.validos], ["Ana@ejemplo.com", "bien@ejemplo.com"])
        self.assertEqual(lista.duplicados, ["ana@ejemplo.com"])
        self.assertEqual(lista.invalidos, ["no-es-correo", "a@x.com, b@x.com"])

    def test_rechaza_correos_mal_formados(self):
        for malo in ["", "a@b", "a@b.", ".a@b.com", "a..b@ejemplo.com", "a@b..com", "a b@ejemplo.com", "a@ejemplo.com\n"]:
            self.assertFalse(es_email(malo), malo)
        self.assertTrue(es_email("persona+aviso@ejemplo.com"))

    def test_lee_excel_cp1252(self):
        ruta = Path(tempfile.mkdtemp()) / "lista.csv"
        self.addCleanup(lambda: ruta.unlink(missing_ok=True))
        ruta.write_bytes("nombre,correo\nAna Gómez,ana@ejemplo.com\n".encode("cp1252"))
        lista = parse_destinatarios(leer_archivo(ruta))
        self.assertEqual(lista.validos[0].nombre, "Ana Gómez")

    def test_ejemplo_del_repositorio(self):
        ruta = Path(__file__).resolve().parents[1] / "ejemplo" / "destinatarios.csv"
        lista = parse_destinatarios(leer_archivo(ruta))
        self.assertEqual(len(lista.validos), 3)


if __name__ == "__main__":
    unittest.main()
