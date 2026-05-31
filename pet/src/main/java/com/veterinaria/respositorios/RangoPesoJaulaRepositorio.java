package com.veterinaria.respositorios;

import com.veterinaria.modelos.RangoPesoJaula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RangoPesoJaulaRepositorio extends JpaRepository<RangoPesoJaula, Long> {
    List<RangoPesoJaula> findByEspecieId(Long especieId);
}
