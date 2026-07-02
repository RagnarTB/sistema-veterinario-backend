package com.veterinaria.respositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.modelos.AtencionMedica;

public interface AtencionMedicaRepositorio extends JpaRepository<AtencionMedica, Long> {
    boolean existsByCitaIdAndPacienteId(Long citaId, Long pacienteId);
    int countByCitaId(Long citaId);
    java.util.Optional<AtencionMedica> findByCitaIdAndPacienteId(Long citaId, Long pacienteId);
    java.util.Optional<AtencionMedica> findTopByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);
    org.springframework.data.domain.Page<AtencionMedica> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId, org.springframework.data.domain.Pageable pageable);
}
