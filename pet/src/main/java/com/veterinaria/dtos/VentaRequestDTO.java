package com.veterinaria.dtos;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VentaRequestDTO {
    @Positive(message = "El ID del cliente debe ser positivo")
    private Long clienteId; // Ahora opcional (venta sin cliente registrado)

    @NotNull(message = "La sede es obligatoria")
    @Positive(message = "El ID de sede debe ser positivo")
    private Long sedeId;

    // MetodoPago legacy: si viene, se guarda para compatibilidad.
    // Los pagos reales se registran vía POST /api/ventas/{id}/pago
    private com.veterinaria.modelos.Enums.MetodoPago metodoPago;

    // Tipo de comprobante
    private com.veterinaria.modelos.Enums.TipoComprobante tipoComprobante;

    @jakarta.validation.constraints.Size(max = 20)
    private String ruc;

    @jakarta.validation.constraints.Size(max = 255)
    private String razonSocial;

    @jakarta.validation.constraints.Size(max = 255)
    private String direccionFacturacion;

    // Opcional: vincular con una cita (cobro de servicios desde Atenciones)
    @Positive(message = "El ID de cita debe ser positivo")
    private Long citaId;

    // Lista de pagos inmediatos (split payment)
    @Valid
    private List<PagoRequestDTO> pagos;

    @NotEmpty(message = "La venta debe tener al menos un detalle")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;
}