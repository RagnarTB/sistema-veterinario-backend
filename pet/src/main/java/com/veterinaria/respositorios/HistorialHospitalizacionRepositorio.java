package com.veterinaria.respositorios;

import com.veterinaria.modelos.HistorialHospitalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialHospitalizacionRepositorio extends JpaRepository<HistorialHospitalizacion, Long> {
    List<HistorialHospitalizacion> findByHospitalizacionIdOrderByFechaHoraDesc(Long hospitalizacionId);
}
