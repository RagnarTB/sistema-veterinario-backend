package com.veterinaria.dtos;

import java.math.BigDecimal;

import com.veterinaria.modelos.Enums.MetodoPago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagoRequestDTO {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    private String referencia; // Opcional: nro de operación Yape/Plin

    @NotNull(message = "La sede es obligatoria para registrar el pago en la caja correspondiente")
    private Long sedeId;
}
