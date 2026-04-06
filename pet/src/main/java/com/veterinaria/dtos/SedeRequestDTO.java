package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SedeRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    private String telefono;
}
