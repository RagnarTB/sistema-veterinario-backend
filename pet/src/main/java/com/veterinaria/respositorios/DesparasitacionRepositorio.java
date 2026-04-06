package com.veterinaria.respositorios;

import com.veterinaria.modelos.Desparasitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesparasitacionRepositorio extends JpaRepository<Desparasitacion, Long> {
    List<Desparasitacion> findByPacienteIdOrderByFechaAplicacionDesc(Long pacienteId);
}
