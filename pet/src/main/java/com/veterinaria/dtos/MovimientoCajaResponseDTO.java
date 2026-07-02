package com.veterinaria.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.veterinaria.modelos.Enums.MetodoPago;
import com.veterinaria.modelos.Enums.TipoMovimiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovimientoCajaResponseDTO {
    private Long id;
    private String concepto;
    private BigDecimal monto;
    private TipoMovimiento tipoMovimiento;
    private LocalDateTime fechaHora;
    private MetodoPago metodoPago;
}
