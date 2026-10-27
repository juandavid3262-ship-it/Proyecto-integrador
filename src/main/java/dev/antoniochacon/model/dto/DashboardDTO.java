package dev.antoniochacon.model.dto;

import dev.antoniochacon.model.entity.Repuesto;
import java.time.LocalDate;
import java.util.List;

public record DashboardDTO(
        LocalDate desde,
        LocalDate hasta,
        List<ConteoDTO> porEstado,
        List<SerieDTO> porMesYTipo,
        List<Repuesto> stockBajo
) {
}
