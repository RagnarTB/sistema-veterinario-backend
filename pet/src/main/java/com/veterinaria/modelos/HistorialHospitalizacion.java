package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "historial_hospitalizacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class HistorialHospitalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalizacion_id", nullable = false)
    private Hospitalizacion hospitalizacion;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    // MONITOREO, TRASLADO, APLICACION_MEDICAMENTO, INGRESO, ALTA
    @Column(nullable = false)
    private String tipoAccion;

    // Solo si es un medicamento
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private BigDecimal cantidadAplicada;

    // CLINICA (cobra) o CLIENTE (no cobra)
    private String origenMedicamento; 

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado registradoPor;
}

