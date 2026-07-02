package com.veterinaria.controladores;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veterinaria.dtos.AuditoriaKardexDTO;
import com.veterinaria.servicios.AuditoriaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes/{pacienteId}/kardex")
public class AuditoriaController {

    private final AuditoriaServicio auditoriaServicio;

    public AuditoriaController(AuditoriaServicio auditoriaServicio) {
        this.auditoriaServicio = auditoriaServicio;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditoriaKardexDTO>> obtenerKardex(@PathVariable Long pacienteId) {
        List<AuditoriaKardexDTO> kardex = auditoriaServicio.obtenerKardexPorPaciente(pacienteId);
        return ResponseEntity.ok(kardex);
    }
}
