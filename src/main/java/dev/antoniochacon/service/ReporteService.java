package dev.antoniochacon.service;

import dev.antoniochacon.model.entity.Reparacion;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String[] COLUMNAS = {"Fecha", "Tipo", "Estado", "Placa", "Modelo", "Marca", "Mecánico"};
    private static final float[] ANCHOS = {78, 130, 78, 70, 110, 90, 120};

    private final ReparacionService reparaciones;
    private final byte[] fuenteRegular;
    private final byte[] fuenteNegrita;

    public ReporteService(ReparacionService reparaciones) throws IOException {
        this.reparaciones = reparaciones;
        this.fuenteRegular = leerFuente("/fonts/DejaVuSans.ttf");
        this.fuenteNegrita = leerFuente("/fonts/DejaVuSans-Bold.ttf");
    }

    public byte[] generarPdf(LocalDate desde, LocalDate hasta) {
        List<Reparacion> filas = reparaciones.buscarPorPeriodo(desde, hasta);
        try (PDDocument documento = new PDDocument()) {
            PDFont regular = PDType0Font.load(documento, new java.io.ByteArrayInputStream(fuenteRegular));
            PDFont negrita = PDType0Font.load(documento, new java.io.ByteArrayInputStream(fuenteNegrita));
            PDRectangle hoja = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            Escritor escritor = new Escritor(documento, hoja, regular, negrita, desde, hasta);
            escritor.encabezado();
            for (Reparacion reparacion : filas) {
                escritor.fila(new String[] {
                        reparacion.getFecha().format(FECHA),
                        reparacion.getTipo(),
                        reparacion.getEstado().etiqueta(),
                        reparacion.getVehiculo().getPlaca(),
                        reparacion.getVehiculo().getModelo().getNombre(),
                        reparacion.getVehiculo().getModelo().getMarca(),
                        reparacion.getMecanico().getNombre()
                });
            }
            if (filas.isEmpty()) {
                escritor.nota("No hay reparaciones en el periodo seleccionado.");
            }
            escritor.cerrar();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo construir el PDF", ex);
        }
    }

    public byte[] generarCsv(LocalDate desde, LocalDate hasta) {
        List<Reparacion> filas = reparaciones.buscarPorPeriodo(desde, hasta);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("fecha,tipo,estado,placa,modelo,marca,mecanico\n");
        for (Reparacion reparacion : filas) {
            csv.append(celda(reparacion.getFecha().format(FECHA))).append(',');
            csv.append(celda(reparacion.getTipo())).append(',');
            csv.append(celda(reparacion.getEstado().etiqueta())).append(',');
            csv.append(celda(reparacion.getVehiculo().getPlaca())).append(',');
            csv.append(celda(reparacion.getVehiculo().getModelo().getNombre())).append(',');
            csv.append(celda(reparacion.getVehiculo().getModelo().getMarca())).append(',');
            csv.append(celda(reparacion.getMecanico().getNombre())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String celda(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private byte[] leerFuente(String ruta) throws IOException {
        try (InputStream entrada = getClass().getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró la fuente " + ruta);
            }
            return entrada.readAllBytes();
        }
    }

    private static final class Escritor {
        private final PDDocument documento;
        private final PDRectangle hoja;
        private final PDFont regular;
        private final PDFont negrita;
        private final LocalDate desde;
        private final LocalDate hasta;
        private PDPageContentStream flujo;
        private float y;

        private Escritor(PDDocument documento, PDRectangle hoja, PDFont regular, PDFont negrita, LocalDate desde, LocalDate hasta) {
            this.documento = documento;
            this.hoja = hoja;
            this.regular = regular;
            this.negrita = negrita;
            this.desde = desde;
            this.hasta = hasta;
        }

        private void encabezado() throws IOException {
            nuevaPagina();
        }

        private void fila(String[] valores) throws IOException {
            if (y < 48) {
                nuevaPagina();
            }
            float x = 36;
            flujo.setNonStrokingColor(Color.BLACK);
            flujo.setFont(regular, 9);
            for (int i = 0; i < valores.length; i++) {
                flujo.beginText();
                flujo.newLineAtOffset(x, y);
                flujo.showText(recortar(regular, 9, valores[i], ANCHOS[i] - 8));
                flujo.endText();
                x += ANCHOS[i];
            }
            y -= 16;
        }

        private void nota(String texto) throws IOException {
            flujo.setFont(regular, 11);
            flujo.beginText();
            flujo.newLineAtOffset(36, y);
            flujo.showText(texto);
            flujo.endText();
        }

        private void nuevaPagina() throws IOException {
            if (flujo != null) {
                flujo.close();
            }
            PDPage pagina = new PDPage(hoja);
            documento.addPage(pagina);
            flujo = new PDPageContentStream(documento, pagina);
            flujo.setNonStrokingColor(new Color(28, 47, 36));
            flujo.addRect(0, hoja.getHeight() - 64, hoja.getWidth(), 64);
            flujo.fill();
            flujo.setNonStrokingColor(Color.WHITE);
            flujo.setFont(negrita, 16);
            flujo.beginText();
            flujo.newLineAtOffset(36, hoja.getHeight() - 30);
            flujo.showText("Historial de reparaciones");
            flujo.endText();
            flujo.setFont(regular, 10);
            flujo.beginText();
            flujo.newLineAtOffset(36, hoja.getHeight() - 48);
            flujo.showText("Periodo " + desde.format(FECHA) + " a " + hasta.format(FECHA));
            flujo.endText();

            y = hoja.getHeight() - 88;
            float x = 36;
            flujo.setNonStrokingColor(new Color(90, 58, 36));
            flujo.setFont(negrita, 9);
            for (int i = 0; i < COLUMNAS.length; i++) {
                flujo.beginText();
                flujo.newLineAtOffset(x, y);
                flujo.showText(COLUMNAS[i]);
                flujo.endText();
                x += ANCHOS[i];
            }
            y -= 18;
        }

        private void cerrar() throws IOException {
            if (flujo != null) {
                flujo.close();
            }
        }

        private String recortar(PDFont fuente, float tamano, String texto, float ancho) throws IOException {
            if (texto == null) {
                return "";
            }
            String actual = texto;
            while (fuente.getStringWidth(actual) / 1000 * tamano > ancho && actual.length() > 1) {
                actual = actual.substring(0, actual.length() - 1);
            }
            if (!actual.equals(texto) && actual.length() > 1) {
                actual = actual.substring(0, actual.length() - 1) + "…";
            }
            return actual;
        }
    }
}
