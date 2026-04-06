package com.veterinaria.dtos;

import lombok.Data;

@Data
public class DetalleRecetaResponseDTO {
    private Long id;
    private String medicamento;
    private String dosis;
    private String frecuencia;
    private Integer duracionDias;
    private Long productoId;
    private String productoNombre;
}
