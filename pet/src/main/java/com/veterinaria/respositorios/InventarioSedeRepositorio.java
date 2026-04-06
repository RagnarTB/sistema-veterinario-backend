package com.veterinaria.respositorios;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.veterinaria.modelos.InventarioSede;

public interface InventarioSedeRepositorio extends JpaRepository<InventarioSede, Long> {
    Optional<InventarioSede> findByProductoIdAndSedeId(Long productoId, Long sedeId);
    List<InventarioSede> findBySedeId(Long sedeId);
}
