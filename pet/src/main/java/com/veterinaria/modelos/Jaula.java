package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jaulas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Jaula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_jaula_id", nullable = false)
    private CategoriaJaula categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tamano_jaula_id", nullable = false)
    private TamanoJaula tamano;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean alertaContagio = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}
