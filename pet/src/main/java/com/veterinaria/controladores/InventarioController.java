package com.veterinaria.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import com.veterinaria.dtos.InventarioRequestDTO;
import com.veterinaria.servicios.InventarioServicio;
import com.veterinaria.modelos.InventarioSede;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {
    
    private final InventarioServicio inventarioServicio;

    public InventarioController(InventarioServicio inventarioServicio) {
        this.inventarioServicio = inventarioServicio;
    }

    @PostMapping("/ajustar")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public InventarioSede ajustar(@Valid @RequestBody InventarioRequestDTO dto) {
        return inventarioServicio.actualizarInventario(dto);
    }
}
