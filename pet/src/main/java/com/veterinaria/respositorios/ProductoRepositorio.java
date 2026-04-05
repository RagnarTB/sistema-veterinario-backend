package com.veterinaria.respositorios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.modelos.Producto;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {
    // 1. Para el módulo de "Administración" (El admin ve todos, activos e
    // inactivos)
    // Ya lo tienes gratis con el findAll(Pageable pageable) que heredas de
    // JpaRepository.

    // 2. Para el módulo de "Nueva Venta" (El cajero solo puede vender productos
    // activos)
    Page<Producto> findByActivoTrue(Pageable pageable);
}
