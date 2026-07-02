package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitoreos_hospitalizacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class MonitoreoHospitalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(precision = 6, scale = 2)
    private BigDecimal temperatura;

    private Integer frecuenciaCardiaca;

    private String apetito;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalizacion_id", nullable = false)
    private Hospitalizacion hospitalizacion;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
}
