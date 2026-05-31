package com.veterinaria.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CajaEstadoResponseDTO {
    private boolean abierta;
    private Long cajaId;
    private BigDecimal saldoInicial;
    private LocalDateTime fechaApertura;
    private String responsableNombre;
}
