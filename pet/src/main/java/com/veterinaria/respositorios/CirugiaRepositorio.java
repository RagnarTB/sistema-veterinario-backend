package com.veterinaria.respositorios;

import com.veterinaria.modelos.Cirugia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CirugiaRepositorio extends JpaRepository<Cirugia, Long> {
    List<Cirugia> findByPacienteIdOrderByFechaHoraFijadaDesc(Long pacienteId);
}
