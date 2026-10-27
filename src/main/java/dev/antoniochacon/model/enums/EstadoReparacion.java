package dev.antoniochacon.model.enums;

public enum EstadoReparacion {
    TERMINADO,
    CANCELADO,
    EN_PROCESO;

    public String etiqueta() {
        return switch (this) {
            case TERMINADO -> "Terminado";
            case CANCELADO -> "Cancelado";
            case EN_PROCESO -> "En proceso";
        };
    }
}
