package com.veterinaria.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HistorialHospitalizacionResponseDTO {
    private Long id;
    private Long hospitalizacionId;
    private LocalDateTime fechaHora;
    private String descripcion;
    private String tipoAccion;
    private Long productoId;
    private String productoNombre;
    private BigDecimal cantidadAplicada;
    private String origenMedicamento;
    private Long registradoPorId;
    private String registradoPorNombre;
}
