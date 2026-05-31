package com.veterinaria.modelos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.veterinaria.modelos.Enums.EstadoCita;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "citas")
public class Cita {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cita)) return false;
        Cita cita = (Cita) o;
        return getId() != null && getId().equals(cita.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return Id;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;
    private String motivo;
    private EstadoCita estado;
    // De un solo paciente a una lista (Muchos a Muchos)
    @ManyToMany
    @JoinTable(name = "cita_pacientes", joinColumns = @JoinColumn(name = "cita_id"), inverseJoinColumns = @JoinColumn(name = "paciente_id"))
    @ToString.Exclude
    private List<Paciente> pacientes;

    @OneToOne(mappedBy = "cita", cascade = CascadeType.ALL)
    @ToString.Exclude
    private AtencionMedica atencionMedica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    @ToString.Exclude
    private ServicioMedico servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id", nullable = false)
    @ToString.Exclude
    private Empleado veterinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    @ToString.Exclude
    private Sede sede;
}
