package com.veterinaria.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RecetaRequestDTO {

    private String indicacionesGenerales;

    @NotNull(message = "La atención médica es obligatoria")
    private Long atencionMedicaId;

    @NotEmpty(message = "La receta debe tener al menos un detalle")
    @Valid
    private List<DetalleRecetaRequestDTO> detalles;
}
