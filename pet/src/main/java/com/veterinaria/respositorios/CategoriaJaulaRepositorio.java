package com.veterinaria.respositorios;

import com.veterinaria.modelos.CategoriaJaula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaJaulaRepositorio extends JpaRepository<CategoriaJaula, Long> {
    List<CategoriaJaula> findByActivoTrue();
}
