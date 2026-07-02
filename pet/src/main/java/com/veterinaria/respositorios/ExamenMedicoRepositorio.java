package com.veterinaria.respositorios;

import com.veterinaria.modelos.ExamenMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamenMedicoRepositorio extends JpaRepository<ExamenMedico, Long> {
    List<ExamenMedico> findByAtencionMedicaId(Long atencionMedicaId);
}
