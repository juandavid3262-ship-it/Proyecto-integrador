package dev.antoniochacon.model.enums;

import java.util.Locale;

public enum ColorPlaca {
    AMARILLO,
    BLANCO,
    ROJO,
    AZUL,
    VERDE;

    public String etiqueta() {
        String nombre = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }
}
