package com.veterinaria.modelos;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
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
@Table(name = "clientes")
public class Cliente {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente cliente = (Cliente) o;
        return getId() != null && getId().equals(cliente.getId());
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


    // "Un cliente tiene Muchos pacientes"
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true) // mappedBy = "cliente": Le dice a
                                                                                      // Spring "No crees una tabla
                                                                                      // nueva para esto. El dueño de
                                                                                      // esta relación es la variable
                                                                                      // cliente que está en la clase
                                                                                      // Paciente".
    @ToString.Exclude
    private List<Paciente> pacientes;// cascade = CascadeType.ALL, orphanRemoval = true: Significa que si eliminas a
                                     // un Cliente de la base de datos, automáticamente se eliminarán todas sus
                                     // mascotas para no dejar "perritos huérfanos" en el sistema sin dueño.


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean esInvitado = false; // Por defecto es false para no romper los clientes actuales

    @Column(nullable = false)
    private Boolean activo = true;

    // ===== Control de crédito / deuda =====
    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal deudaAcumulada = java.math.BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal limiteCredito = new java.math.BigDecimal("200.00");

}
