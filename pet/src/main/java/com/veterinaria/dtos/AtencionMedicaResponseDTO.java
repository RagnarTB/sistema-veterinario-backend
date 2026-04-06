package com.veterinaria.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtencionMedicaResponseDTO {
    private Long id;
    private String sintomas;
    private String diagnostico;
    private String tratamiento;
    private BigDecimal peso;
    private BigDecimal temperatura;
    private Integer frecuenciaCardiaca;
    private String resumenIaCliente;
    private Long citaId;
    private Long veterinarioId;
    private Boolean activo;

}
