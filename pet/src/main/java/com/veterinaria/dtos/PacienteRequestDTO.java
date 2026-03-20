package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PacienteRequestDTO {
    @NotBlank(message = "El nombre del paciente es obligatorio")
    private String nombre;

    @NotBlank(message = "La especie es obligatoria")
    private String especie;

    private String raza;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;
}
