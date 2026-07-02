package com.veterinaria.controladores;

import com.veterinaria.dtos.DesparasitacionRequestDTO;
import com.veterinaria.dtos.DesparasitacionResponseDTO;
import com.veterinaria.servicios.DesparasitacionServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/desparasitaciones")
@RequiredArgsConstructor
public class DesparasitacionController {

    private final DesparasitacionServicio desparasitacionServicio;

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_REGISTROS_CLINICOS')")
    public ResponseEntity<DesparasitacionResponseDTO> registrarDesparasitacion(@Valid @RequestBody DesparasitacionRequestDTO requestDTO) {
        DesparasitacionResponseDTO response = desparasitacionServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAuthority('VER_PACIENTES')")
    public ResponseEntity<List<DesparasitacionResponseDTO>> listarDesparasitacionesPorPaciente(@PathVariable Long pacienteId) {
        List<DesparasitacionResponseDTO> response = desparasitacionServicio.listarPorPaciente(pacienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proximas")
    @PreAuthorize("hasAuthority('VER_HISTORIAL')")
    public ResponseEntity<List<DesparasitacionResponseDTO>> listarProximasDosis() {
        List<DesparasitacionResponseDTO> response = desparasitacionServicio.listarProximasDosis();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/proxima-dosis")
    @PreAuthorize("hasAuthority('CREAR_REGISTROS_CLINICOS')")
    public ResponseEntity<DesparasitacionResponseDTO> actualizarProximaDosis(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        
        java.time.LocalDate nuevaFecha = null;
        if (body.get("fecha") != null && !body.get("fecha").trim().isEmpty()) {
            nuevaFecha = java.time.LocalDate.parse(body.get("fecha"));
        }
        
        DesparasitacionResponseDTO response = desparasitacionServicio.actualizarProximaDosis(id, nuevaFecha);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarDesparasitacion(
            @PathVariable Long id,
            @RequestParam String motivo) {
        desparasitacionServicio.eliminar(id, motivo);
        return ResponseEntity.noContent().build();
    }
}
