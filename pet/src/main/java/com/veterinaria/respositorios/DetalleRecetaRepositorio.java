package com.veterinaria.respositorios;

import com.veterinaria.modelos.DetalleReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleRecetaRepositorio extends JpaRepository<DetalleReceta, Long> {
}
