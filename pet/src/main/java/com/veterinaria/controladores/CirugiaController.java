package com.veterinaria.controladores;

import com.veterinaria.dtos.CirugiaRequestDTO;
import com.veterinaria.dtos.CirugiaResponseDTO;
import com.veterinaria.servicios.CirugiaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cirugias")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
public class CirugiaController {

    private final CirugiaServicio cirugiaServicio;

    @PostMapping
    public ResponseEntity<CirugiaResponseDTO> registrarCirugia(@Valid @RequestBody CirugiaRequestDTO requestDTO) {
        CirugiaResponseDTO response = cirugiaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CirugiaResponseDTO> actualizarCirugia(@PathVariable Long id, @Valid @RequestBody CirugiaRequestDTO requestDTO) {
        CirugiaResponseDTO response = cirugiaServicio.actualizar(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<CirugiaResponseDTO> cambiarEstadoCirugia(
            @PathVariable Long id, 
            @RequestBody String nuevoEstado) {
        CirugiaResponseDTO response = cirugiaServicio.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CirugiaResponseDTO> obtenerCirugia(@PathVariable Long id) {
        return ResponseEntity.ok(cirugiaServicio.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<CirugiaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(cirugiaServicio.listarTodas());
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<CirugiaResponseDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(cirugiaServicio.listarPorPaciente(pacienteId));
    }
}
