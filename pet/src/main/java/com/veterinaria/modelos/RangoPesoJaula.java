package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "rangos_peso_jaula")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RangoPesoJaula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pesoMinimo;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pesoMaximo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tamano_jaula_id", nullable = false)
    private TamanoJaula tamanoJaula;
}
