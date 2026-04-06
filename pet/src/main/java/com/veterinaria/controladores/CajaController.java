package com.veterinaria.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veterinaria.dtos.CajaRequestDTO;
import com.veterinaria.dtos.CierreCajaResponseDTO;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.servicios.CajaServicio;
import com.veterinaria.servicios.EmpleadoAutenticadoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaServicio cajaServicio;

    @Autowired
    private EmpleadoAutenticadoService empleadoAutenticadoService;

    @PostMapping("/abrir")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> abrirCaja(@Valid @RequestBody CajaRequestDTO dto) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        cajaServicio.abrirCaja(dto, empleadoActual);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/cerrar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CierreCajaResponseDTO> cerrarCaja(@RequestParam Long sedeId) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        CierreCajaResponseDTO resumen = cajaServicio.cerrarCaja(sedeId, empleadoActual);
        return ResponseEntity.ok(resumen);
    }
}