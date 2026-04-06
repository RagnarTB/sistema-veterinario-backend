package com.veterinaria.respositorios;

import com.veterinaria.modelos.ConsentimientoInformado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsentimientoRepositorio extends JpaRepository<ConsentimientoInformado, Long> {
    Optional<ConsentimientoInformado> findByCirugiaId(Long cirugiaId);
}
