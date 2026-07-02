package com.veterinaria.controladores;

import com.veterinaria.dtos.VacunaRequestDTO;
import com.veterinaria.dtos.VacunaResponseDTO;
import com.veterinaria.servicios.VacunaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vacunas")
@RequiredArgsConstructor
public class VacunaController {

    private final VacunaServicio vacunaServicio;

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_REGISTROS_CLINICOS')")
    public ResponseEntity<VacunaResponseDTO> registrarVacuna(@Valid @RequestBody VacunaRequestDTO requestDTO) {
        VacunaResponseDTO response = vacunaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAuthority('VER_PACIENTES')")
    public ResponseEntity<List<VacunaResponseDTO>> listarVacunasPorPaciente(@PathVariable Long pacienteId) {
        List<VacunaResponseDTO> response = vacunaServicio.listarPorPaciente(pacienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proximas")
    @PreAuthorize("hasAuthority('VER_HISTORIAL')")
    public ResponseEntity<List<VacunaResponseDTO>> listarProximasDosis() {
        List<VacunaResponseDTO> response = vacunaServicio.listarProximasDosis();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/proxima-dosis")
    @PreAuthorize("hasAuthority('CREAR_REGISTROS_CLINICOS')")
    public ResponseEntity<VacunaResponseDTO> actualizarProximaDosis(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        
        java.time.LocalDate nuevaFecha = null;
        if (body.get("fecha") != null && !body.get("fecha").trim().isEmpty()) {
            nuevaFecha = java.time.LocalDate.parse(body.get("fecha"));
        }
        
        VacunaResponseDTO response = vacunaServicio.actualizarProximaDosis(id, nuevaFecha);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarVacuna(
            @PathVariable Long id,
            @RequestParam String motivo) {
        vacunaServicio.eliminar(id, motivo);
        return ResponseEntity.noContent().build();
    }
}
