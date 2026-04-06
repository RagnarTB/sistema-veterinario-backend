package com.veterinaria.respositorios;

import com.veterinaria.modelos.MonitoreoHospitalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoreoHospitalizacionRepositorio extends JpaRepository<MonitoreoHospitalizacion, Long> {
    List<MonitoreoHospitalizacion> findByHospitalizacionIdOrderByFechaHoraDesc(Long hospitalizacionId);
}
