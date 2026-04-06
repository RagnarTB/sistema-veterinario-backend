package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class VacunaRequestDTO {

    @NotBlank(message = "El nombre de la vacuna es obligatorio")
    @Size(max = 255, message = "El nombre de la vacuna no puede superar 255 caracteres")
    private String nombreVacuna;

    @NotNull(message = "La fecha de aplicación es obligatoria")
    @PastOrPresent(message = "La fecha de aplicación no puede ser en el futuro")
    private LocalDate fechaAplicacion;

    @FutureOrPresent(message = "La próxima dosis no puede ser en el pasado")
    private LocalDate fechaProximaDosis;

    @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
    private String observaciones;

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser positivo")
    private Long pacienteId;

    @NotNull(message = "El ID del empleado es obligatorio")
    @Positive(message = "El ID del empleado debe ser positivo")
    private Long empleadoId;
}
