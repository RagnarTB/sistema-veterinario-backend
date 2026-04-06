package com.veterinaria.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DetalleVentaRequestDTO {

    // Opcional individualmente: el frontend manda UNO de los dos.
    // Si viene ninguno o ambos, el servicio lanza BAD_REQUEST.
    @Positive(message = "El ID del producto debe ser positivo")
    private Long productoId;

    @Positive(message = "El ID del servicio debe ser positivo")
    private Long servicioId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", inclusive = true, message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
}