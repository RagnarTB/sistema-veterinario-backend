package com.veterinaria.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonitoreoHospitalizacionResponseDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private BigDecimal temperatura;
    private Integer frecuenciaCardiaca;
    private String apetito;
    private String observaciones;
    private Long hospitalizacionId;
    private Long empleadoId;
    private String empleadoNombre;
}
