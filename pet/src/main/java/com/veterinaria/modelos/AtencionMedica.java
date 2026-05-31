package com.veterinaria.modelos;

import java.math.BigDecimal;

import org.hibernate.envers.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "atenciones_medicas")
@Audited // guarda versiones de cambios mejor dicho historial
public class AtencionMedica {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtencionMedica)) return false;
        AtencionMedica that = (AtencionMedica) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sintomas;
    private String diagnostico;
    private String tratamiento;
    @jakarta.persistence.Column(precision = 8, scale = 3)
    private BigDecimal peso;
    @jakarta.persistence.Column(precision = 6, scale = 2)
    private BigDecimal temperatura;
    private Integer frecuenciaCardiaca;
    private String resumenIaCliente;

    @jakarta.persistence.Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @jakarta.persistence.Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    @ToString.Exclude
    private Cita cita;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    @ToString.Exclude
    private Empleado veterinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ToString.Exclude
    private Paciente paciente;
}
