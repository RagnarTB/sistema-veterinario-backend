package com.veterinaria.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private LocalDateTime fechaHora;
    private BigDecimal total;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    private com.veterinaria.modelos.Enums.EstadoVenta estado;
    private com.veterinaria.modelos.Enums.MetodoPago metodoPago; // legacy
    private com.veterinaria.modelos.Enums.TipoComprobante tipoComprobante;
    private String ruc;
    private String razonSocial;
    private String direccionFacturacion;
    private Long citaId;
    private Long hospitalizacionId;
    private String estadoHospitalizacion;
    private Long cajaId;
    private List<DetalleVentaResponseDTO> detalles;
    private List<PagoResponseDTO> pagos;
}