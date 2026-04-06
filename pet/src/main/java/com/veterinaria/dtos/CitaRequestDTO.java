package com.veterinaria.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List; // ¡No olvides este import!

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CitaRequestDTO {

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "No puedes agendar una cita en el pasado")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "El ID del servicio es obligatorio")
    @Positive(message = "El ID del servicio debe ser positivo")
    private Long servicioId;
    @NotNull(message = "El ID del veterinario es obligatorio")
    @Positive(message = "El ID del veterinario debe ser positivo")
    private Long veterinarioId;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String motivo;

    // ¡EL CAMBIO! Pasamos de un Long a una List<Long>
    @NotEmpty(message = "La cita debe tener al menos un paciente")
    private List<@NotNull(message = "El ID del paciente no puede ser nulo") @Positive(message = "El ID del paciente debe ser positivo") Long> pacienteIds;

    @NotNull(message = "El ID de sede es obligatorio")
    @Positive(message = "El ID de sede debe ser positivo")
    private Long sedeId;
}