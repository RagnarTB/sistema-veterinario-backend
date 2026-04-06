package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DesparasitacionRequestDTO {

    @NotBlank(message = "El tipo de desparasitación es obligatorio")
    private String tipo;

    @NotBlank(message = "El producto utilizado es obligatorio")
    private String productoUtilizado;

    @NotNull(message = "El peso al momento es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private BigDecimal pesoAlMomento;

    @NotNull(message = "La fecha de aplicación es obligatoria")
    private LocalDate fechaAplicacion;

    private LocalDate fechaProximaDosis;

    private String observaciones;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long empleadoId;
}
