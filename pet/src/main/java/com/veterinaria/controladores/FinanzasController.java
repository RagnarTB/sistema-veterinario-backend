package com.veterinaria.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veterinaria.dtos.VentaResponseDTO;
import com.veterinaria.servicios.VentaServicio;

@RestController
@RequestMapping("/api/finanzas")
public class FinanzasController {

    @Autowired
    private VentaServicio ventaServicio;

    @GetMapping("/deudas")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Page<VentaResponseDTO>> listarVentasConDeuda(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        Page<VentaResponseDTO> deudas = ventaServicio.listarVentasConDeuda(sedeId, query, pageable);
        return ResponseEntity.ok(deudas);
    }
}
