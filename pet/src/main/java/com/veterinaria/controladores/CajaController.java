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
    @PreAuthorize("hasAuthority('VER_CAJA')")
    public ResponseEntity<Void> abrirCaja(@Valid @RequestBody CajaRequestDTO dto) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        cajaServicio.abrirCaja(dto, empleadoActual);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/movimiento")
    @PreAuthorize("hasAuthority('VER_CAJA')")
    public ResponseEntity<Void> registrarMovimiento(@Valid @RequestBody com.veterinaria.dtos.MovimientoCajaRequestDTO dto) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        cajaServicio.registrarMovimiento(dto, empleadoActual);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/cerrar")
    @PreAuthorize("hasAuthority('ABRIR_CERRAR_CAJA')")
    public ResponseEntity<CierreCajaResponseDTO> cerrarCaja(@RequestParam Long sedeId) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        CierreCajaResponseDTO resumen = cajaServicio.cerrarCaja(sedeId, empleadoActual);
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAuthority('VER_CAJA')")
    public ResponseEntity<com.veterinaria.dtos.CajaEstadoResponseDTO> obtenerEstadoCaja(@RequestParam Long sedeId) {
        Empleado empleadoActual = empleadoAutenticadoService.obtenerEmpleadoActual();
        com.veterinaria.dtos.CajaEstadoResponseDTO estado = cajaServicio.obtenerEstadoCaja(sedeId, empleadoActual);
        return ResponseEntity.ok(estado);
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAuthority('VER_CAJA')")
    public ResponseEntity<org.springframework.data.domain.Page<com.veterinaria.dtos.CajaHistorialResponseDTO>> listarCajasCerradas(
            @RequestParam Long sedeId,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(cajaServicio.listarCajasCerradas(sedeId, pageable));
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('VER_CAJA')")
    public ResponseEntity<java.util.List<com.veterinaria.dtos.MovimientoCajaResponseDTO>> listarMovimientos(@PathVariable Long id) {
        return ResponseEntity.ok(cajaServicio.listarMovimientos(id));
    }
}