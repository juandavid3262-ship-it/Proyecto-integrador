package dev.antoniochacon.model.dto;

import dev.antoniochacon.model.enums.ColorPlaca;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VehiculoForm {

    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 7, message = "La placa usa el formato ABC-123")
    private String placa;

    @NotNull(message = "El color de la placa es obligatorio")
    private ColorPlaca colorPlaca;

    @NotBlank(message = "El dueño es obligatorio")
    @Size(max = 100, message = "El dueño admite hasta 100 caracteres")
    private String dueno;

    @NotNull(message = "El modelo es obligatorio")
    private Long modeloId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public ColorPlaca getColorPlaca() {
        return colorPlaca;
    }

    public void setColorPlaca(ColorPlaca colorPlaca) {
        this.colorPlaca = colorPlaca;
    }

    public String getDueno() {
        return dueno;
    }

    public void setDueno(String dueno) {
        this.dueno = dueno;
    }

    public Long getModeloId() {
        return modeloId;
    }

    public void setModeloId(Long modeloId) {
        this.modeloId = modeloId;
    }
}
