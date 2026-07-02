package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JaulaRequestDTO {

    @NotBlank(message = "El número de jaula es obligatorio")
    private String numero;

    private Long categoriaId;

    private Long tamanoId;

    private Boolean alertaContagio = false;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotNull(message = "El ID de la sede es obligatorio")
    private Long sedeId;
}
