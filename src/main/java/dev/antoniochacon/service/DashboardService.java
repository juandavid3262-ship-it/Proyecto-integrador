package dev.antoniochacon.service;

import dev.antoniochacon.model.dto.ConteoDTO;
import dev.antoniochacon.model.dto.DashboardDTO;
import dev.antoniochacon.model.dto.SerieDTO;
import dev.antoniochacon.model.enums.EstadoReparacion;
import dev.antoniochacon.repository.ReparacionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ReparacionRepository reparaciones;
    private final RepuestoService repuestos;
    private final ReparacionService reglas;

    public DashboardService(ReparacionRepository reparaciones, RepuestoService repuestos, ReparacionService reglas) {
        this.reparaciones = reparaciones;
        this.repuestos = repuestos;
        this.reglas = reglas;
    }

    public DashboardDTO obtenerResumen(LocalDate desde, LocalDate hasta) {
        reglas.validarRango(desde, hasta);
        List<ConteoDTO> porEstado = reparaciones.contarPorEstado(desde, hasta).stream()
                .map(fila -> {
                    EstadoReparacion estado = (EstadoReparacion) fila[0];
                    return new ConteoDTO(estado.etiqueta(), numero(fila[1]));
                })
                .toList();
        List<SerieDTO> series = reparaciones.contarPorMesYTipo(desde, hasta).stream()
                .map(fila -> new SerieDTO(
                        String.format("%04d-%02d", ((Number) fila[0]).intValue(), ((Number) fila[1]).intValue()),
                        (String) fila[2],
                        numero(fila[3])
                ))
                .toList();
        return new DashboardDTO(desde, hasta, porEstado, series, repuestos.stockBajo());
    }

    private long numero(Object valor) {
        return ((Number) valor).longValue();
    }
}
