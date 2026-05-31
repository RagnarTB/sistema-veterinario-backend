package com.veterinaria.controladores;

import com.veterinaria.dtos.HospitalizacionRequestDTO;
import com.veterinaria.dtos.HospitalizacionResponseDTO;
import com.veterinaria.servicios.HospitalizacionServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitalizaciones")
@RequiredArgsConstructor
public class HospitalizacionController {

    private final HospitalizacionServicio hospitalizacionServicio;

    @PostMapping("/{id}/monitoreo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<com.veterinaria.dtos.HistorialHospitalizacionResponseDTO> registrarMonitoreo(
            @PathVariable Long id,
            @Valid @RequestBody com.veterinaria.dtos.HistorialHospitalizacionRequestDTO requestDTO) {
        if (!id.equals(requestDTO.getHospitalizacionId())) {
            throw new IllegalArgumentException("ID en ruta no coincide con el cuerpo");
        }
        return ResponseEntity.ok(hospitalizacionServicio.registrarMonitoreo(requestDTO));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<java.util.List<HospitalizacionResponseDTO>> listarActivas(@RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(hospitalizacionServicio.listarHospitalizacionesActivas(sedeId));
    }

    @GetMapping("/sugerencia-jaula")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<com.veterinaria.dtos.SugerenciaJaulaResponseDTO> sugerirJaula(
            @RequestParam Long pacienteId,
            @RequestParam Long sedeId,
            @RequestParam(required = false) java.math.BigDecimal pesoActual) {
        return ResponseEntity.ok(hospitalizacionServicio.sugerirJaula(pacienteId, sedeId, pesoActual));
    }

    @PostMapping("/ingreso")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<HospitalizacionResponseDTO> ingresarPaciente(@Valid @RequestBody HospitalizacionRequestDTO requestDTO) {
        HospitalizacionResponseDTO response = hospitalizacionServicio.ingresarPaciente(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/alta")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<HospitalizacionResponseDTO> darDeAlta(@PathVariable Long id) {
        HospitalizacionResponseDTO response = hospitalizacionServicio.darDeAlta(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/traslado")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<HospitalizacionResponseDTO> trasladarPaciente(
            @PathVariable Long id, 
            @Valid @RequestBody com.veterinaria.dtos.TrasladoHospitalizacionDTO requestDTO) {
        HospitalizacionResponseDTO response = hospitalizacionServicio.trasladarPaciente(id, requestDTO.getNuevaJaulaId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/fallecimiento")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<HospitalizacionResponseDTO> registrarFallecimiento(@PathVariable Long id) {
        HospitalizacionResponseDTO response = hospitalizacionServicio.registrarFallecimiento(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/gravedad")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<HospitalizacionResponseDTO> cambiarGravedad(
            @PathVariable Long id,
            @RequestParam String nivel) {
        return ResponseEntity.ok(hospitalizacionServicio.cambiarGravedad(id, nivel));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<java.util.List<com.veterinaria.dtos.HistorialHospitalizacionResponseDTO>> listarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalizacionServicio.listarHistorial(id));
    }
}
