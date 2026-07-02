package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HospitalizacionRequestDTO {

    @NotBlank(message = "El motivo de ingreso es obligatorio")
    private String motivoIngreso;

    private LocalDateTime fechaIngreso;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID de la jaula es obligatorio")
    private Long jaulaId;

    @NotNull(message = "El ID del veterinario es obligatorio")
    private Long empleadoId;

    private Integer frecuenciaMonitoreoHoras = 4;

    @NotNull(message = "El peso actual es obligatorio")
    private BigDecimal pesoActual;
}
