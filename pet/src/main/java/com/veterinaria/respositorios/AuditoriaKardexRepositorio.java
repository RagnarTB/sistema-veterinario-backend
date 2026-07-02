package com.veterinaria.respositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.modelos.AuditoriaKardex;

import java.util.List;

@Repository
public interface AuditoriaKardexRepositorio extends JpaRepository<AuditoriaKardex, Long> {
    List<AuditoriaKardex> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
}
