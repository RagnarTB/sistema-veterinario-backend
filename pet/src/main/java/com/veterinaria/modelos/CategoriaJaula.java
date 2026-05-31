package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categorias_jaula")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaJaula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(precision = 8, scale = 2)
    private java.math.BigDecimal precioPorDia;
}
