package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "cirugias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Cirugia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoCirugia;

    @Column(nullable = false)
    private LocalDateTime fechaHoraFijada;

    @Column(nullable = false)
    private String riesgoOperatorio;

    @Column(nullable = false)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String notasPostOperatorias;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cirujano_id", nullable = false)
    private Empleado cirujano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalizacion_id")
    private Hospitalizacion hospitalizacion;
}
