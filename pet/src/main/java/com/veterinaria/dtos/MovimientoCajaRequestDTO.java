package com.veterinaria.dtos;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class MovimientoCajaRequestDTO {
    @NotNull
    private Long sedeId;
    @NotNull
    private String tipo; // "INGRESO" o "EGRESO"
    @NotNull
    private BigDecimal monto;
    private String concepto;
    private String metodoPago;
}
