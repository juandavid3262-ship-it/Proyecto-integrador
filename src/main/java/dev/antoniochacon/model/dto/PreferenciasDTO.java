package dev.antoniochacon.model.dto;

import java.util.Locale;

public record PreferenciasDTO(String tema, boolean sidebarPlegada) {

    public String temaCss() {
        return tema == null ? "claro" : tema.toLowerCase(Locale.ROOT);
    }

    public static PreferenciasDTO defecto() {
        return new PreferenciasDTO("CLARO", false);
    }
}
