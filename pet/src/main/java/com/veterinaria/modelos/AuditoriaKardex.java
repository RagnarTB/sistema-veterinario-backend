package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_kardex")
@Getter
@Setter
public class AuditoriaKardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario", nullable = false)
    private String usuario; // Email o nombre de quien hizo el cambio

    @Column(name = "accion", nullable = false)
    private String accion; // EDICION o ELIMINACION

    @Column(name = "modulo", nullable = false)
    private String modulo; // VACUNACION, DESPARASITACION, etc.

    @Column(name = "detalle", nullable = false, length = 500)
    private String detalle;

    @Column(name = "motivo", length = 500)
    private String motivo; // Requerido para eliminaciones

    @PrePersist
    public void prePersist() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }
}
