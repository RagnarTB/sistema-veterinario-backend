package com.veterinaria.dtos;

import lombok.Data;
import java.util.List;

@Data
public class RecetaResponseDTO {
    private Long id;
    private String indicacionesGenerales;
    private Long atencionMedicaId;
    private List<DetalleRecetaResponseDTO> detalles;
}
