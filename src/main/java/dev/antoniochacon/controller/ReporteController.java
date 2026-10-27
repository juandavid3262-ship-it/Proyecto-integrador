package dev.antoniochacon.controller;

import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.ReporteService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/reportes/reparaciones")
public class ReporteController {

    private final ReporteService reportes;

    public ReporteController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        byte[] archivo = generar(() -> reportes.generarPdf(desde, hasta));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre("pdf", desde, hasta) + "\"")
                .body(archivo);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        byte[] archivo = generar(() -> reportes.generarCsv(desde, hasta));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre("csv", desde, hasta) + "\"")
                .body(archivo);
    }

    private byte[] generar(Generador generador) {
        try {
            return generador.generar();
        } catch (ReglaNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String nombre(String extension, LocalDate desde, LocalDate hasta) {
        return "reparaciones-" + desde + "-" + hasta + "." + extension;
    }

    @FunctionalInterface
    private interface Generador {
        byte[] generar();
    }
}
