package com.veterinaria.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PacienteRequestDTO {
    @NotBlank(message = "El nombre del paciente es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    // ¡EL CAMBIO! Extirpamos el String y pedimos el ID (NotNull porque es número,
    // no texto)
    @NotNull(message = "El ID de la especie es obligatorio")
    @Positive(message = "El ID de la especie debe ser positivo")
    private Long especieId;

    @Size(max = 150, message = "La raza no puede superar 150 caracteres")
    private String raza;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser en el futuro")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser positivo")
    private Long clienteId;
}
