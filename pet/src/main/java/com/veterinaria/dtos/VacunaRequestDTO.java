package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class VacunaRequestDTO {

    @NotBlank(message = "El nombre de la vacuna es obligatorio")
    private String nombreVacuna;

    @NotNull(message = "La fecha de aplicación es obligatoria")
    private LocalDate fechaAplicacion;

    private LocalDate fechaProximaDosis;

    private String observaciones;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long empleadoId;
}
