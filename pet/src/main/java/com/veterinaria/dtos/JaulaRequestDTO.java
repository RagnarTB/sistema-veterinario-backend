package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JaulaRequestDTO {

    @NotBlank(message = "El número de jaula es obligatorio")
    private String numero;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotNull(message = "El ID de la sede es obligatorio")
    private Long sedeId;
}
