package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.ConteoDTO;
import dev.antoniochacon.model.dto.DashboardDTO;
import dev.antoniochacon.model.dto.SerieDTO;
import dev.antoniochacon.service.DashboardService;
import dev.antoniochacon.service.ReglaNegocioException;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public String ver(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model
    ) {
        if (desde == null && hasta == null) {
            hasta = LocalDate.now();
            desde = LocalDate.of(hasta.getYear(), 1, 1);
        }
        try {
            DashboardDTO resumen = dashboard.obtenerResumen(desde, hasta);
            model.addAttribute("dashboard", resumen);
            model.addAttribute("datosJson", aJson(resumen));
            return "dashboard";
        } catch (ReglaNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String aJson(DashboardDTO resumen) {
        StringBuilder json = new StringBuilder();
        json.append("{\"porEstado\":[");
        for (int i = 0; i < resumen.porEstado().size(); i++) {
            ConteoDTO conteo = resumen.porEstado().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"estado\":\"").append(escapar(conteo.estado())).append("\",\"total\":").append(conteo.total()).append('}');
        }
        json.append("],\"porMesYTipo\":[");
        for (int i = 0; i < resumen.porMesYTipo().size(); i++) {
            SerieDTO serie = resumen.porMesYTipo().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"mes\":\"").append(escapar(serie.mes()))
                    .append("\",\"tipo\":\"").append(escapar(serie.tipo()))
                    .append("\",\"total\":").append(serie.total()).append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
