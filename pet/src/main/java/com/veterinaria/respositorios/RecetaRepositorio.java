package com.veterinaria.respositorios;

import com.veterinaria.modelos.RecetaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecetaRepositorio extends JpaRepository<RecetaMedica, Long> {
    Optional<RecetaMedica> findByAtencionMedicaId(Long atencionMedicaId);
}
