package com.veterinaria.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtencionMedicaRequestDTO {

    @NotNull(message = "el id de la cita es obligatorio")
    @Positive(message = "el id de la cita debe ser positivo")
    private Long citaId;
    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser positivo")
    private Long pacienteId;
    @NotBlank(message = "los sintomas son obligatorios")
    @Size(max = 2000, message = "Los síntomas no pueden superar 2000 caracteres")
    private String sintomas;
    @NotBlank(message = "el diagnostico es obligatorio")
    @Size(max = 2000, message = "El diagnóstico no puede superar 2000 caracteres")
    private String diagnostico;
    @NotBlank(message = "el tratamiento es obligatorio")
    @Size(max = 4000, message = "El tratamiento no puede superar 4000 caracteres")
    private String tratamiento;
    @NotNull(message = "el peso es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El peso debe ser mayor a 0")
    private BigDecimal peso;
    @NotNull(message = "la temperatura es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La temperatura debe ser mayor a 0")
    private BigDecimal temperatura;
    @NotNull(message = "la frecuencia cardiaca es obligatoria")
    @Min(value = 1, message = "La frecuencia cardiaca debe ser mayor a 0")
    private Integer frecuenciaCardiaca;

}
