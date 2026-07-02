package com.veterinaria.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.veterinaria.modelos.Enums.EstadoVenta;
import com.veterinaria.modelos.Enums.TipoComprobante;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Audited
@Table(name = "ventas")
@Data
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHora;

    @jakarta.persistence.Column(precision = 19, scale = 2)
    private BigDecimal total;

    // Monto que ya se pagó (suma de todos los PagoVenta)
    @jakarta.persistence.Column(precision = 19, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    // total - montoPagado
    @jakarta.persistence.Column(precision = 19, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    private EstadoVenta estado;

    @jakarta.persistence.Column(length = 20)
    private String ruc;

    @jakarta.persistence.Column(length = 255)
    private String razonSocial;

    @jakarta.persistence.Column(length = 255)
    private String direccionFacturacion;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private CajaDiaria caja;

    // Campo legacy — se mantiene para compatibilidad con ventas existentes.
    // Las ventas nuevas usarán la tabla pagos_venta.
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private com.veterinaria.modelos.Enums.MetodoPago metodoPago;

    // Tipo de comprobante: BOLETA o FACTURA
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private TipoComprobante tipoComprobante;

    // Vinculación opcional con una cita (para cobro de servicios desde Atenciones)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    // Vinculación opcional con una hospitalización (para cobros de alojamiento y monitoreo)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalizacion_id")
    private Hospitalizacion hospitalizacion;

    // Lista de pagos (split payment + pagos parciales)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    // Método de conveniencia para mantener sincronizada la relación bidireccional
    public void agregarDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    // Recalcular estado de pago basado en los pagos registrados
    public void recalcularPagos() {
        this.montoPagado = pagos.stream()
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.saldoPendiente = this.total.subtract(this.montoPagado);

        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            this.saldoPendiente = BigDecimal.ZERO;
            this.estado = EstadoVenta.PAGADA;
        } else if (this.montoPagado.compareTo(BigDecimal.ZERO) > 0) {
            this.estado = EstadoVenta.PAGADA_PARCIAL;
        }
    }
}

