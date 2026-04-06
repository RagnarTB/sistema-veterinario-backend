package com.veterinaria.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsentimientoRequestDTO {

    @NotNull(message = "El ID de la cirugía es obligatorio")
    private Long cirugiaId;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;
}
