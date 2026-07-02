package com.veterinaria.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CierreCajaResponseDTO {
    private Long cajaId;
    private LocalDateTime fechaCierre;
    private BigDecimal saldoInicial;
    private BigDecimal totalVentas;
    private BigDecimal ingresosExtras;
    private BigDecimal egresosDevoluciones;
    private BigDecimal saldoFinal;
}