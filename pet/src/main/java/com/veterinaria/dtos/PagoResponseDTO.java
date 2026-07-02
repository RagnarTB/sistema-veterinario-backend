package com.veterinaria.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.veterinaria.modelos.Enums.MetodoPago;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoResponseDTO {
    private Long id;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private LocalDateTime fechaPago;
    private String referencia;
    private Long cajaId;
}
