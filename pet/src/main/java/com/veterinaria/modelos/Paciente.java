package com.veterinaria.modelos;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
@Table(name = "pacientes")
public class Paciente {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paciente)) return false;
        Paciente paciente = (Paciente) o;
        return getId() != null && getId().equals(paciente.getId());
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

    private String nombre;
    // Actualizacion de la especie con tabla
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;
    private String raza;
    @Column(length = 20)
    private String sexo = "MACHO";
    @ManyToOne(fetch = FetchType.LAZY) // "Muchos pacientes pertenecen a Un cliente"
    @JoinColumn(name = "cliente_id") // se llamará la columna en la base de datos
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    // EL CAMBIO Actualizamos el lado pasivo de la relación
    @ManyToMany(mappedBy = "pacientes")
    @ToString.Exclude
    private List<Cita> citas;

    @Column(nullable = false)
    private Boolean activo = true;

}
