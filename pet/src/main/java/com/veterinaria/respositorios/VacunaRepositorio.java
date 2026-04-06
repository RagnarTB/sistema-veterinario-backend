package com.veterinaria.respositorios;

import com.veterinaria.modelos.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacunaRepositorio extends JpaRepository<Vacuna, Long> {
    List<Vacuna> findByPacienteIdOrderByFechaAplicacionDesc(Long pacienteId);

    List<Vacuna> findByFechaProximaDosis(LocalDate fecha);
}
