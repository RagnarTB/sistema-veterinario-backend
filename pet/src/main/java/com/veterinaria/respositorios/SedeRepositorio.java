package com.veterinaria.respositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import com.veterinaria.modelos.Sede;

public interface SedeRepositorio extends JpaRepository<Sede, Long> {
    boolean existsByNombre(String nombre);
}
