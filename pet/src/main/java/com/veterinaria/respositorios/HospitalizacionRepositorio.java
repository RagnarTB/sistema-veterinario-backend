package com.veterinaria.respositorios;

import com.veterinaria.modelos.Hospitalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalizacionRepositorio extends JpaRepository<Hospitalizacion, Long> {
    Optional<Hospitalizacion> findByPacienteIdAndEstado(Long pacienteId, String estado);
}
