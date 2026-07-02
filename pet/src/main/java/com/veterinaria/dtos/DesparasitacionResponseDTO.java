package com.veterinaria.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DesparasitacionResponseDTO {
    private Long id;
    private String tipo;
    private String productoUtilizado;
    private BigDecimal pesoAlMomento;
    private LocalDate fechaAplicacion;
    private LocalDate fechaProximaDosis;
    private String observaciones;
    private Long pacienteId;
    private String pacienteNombre;
    private Long empleadoId;
    private String empleadoNombre;
}
