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
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<DesparasitacionResponseDTO> registrarDesparasitacion(@Valid @RequestBody DesparasitacionRequestDTO requestDTO) {
        DesparasitacionResponseDTO response = desparasitacionServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<List<DesparasitacionResponseDTO>> listarDesparasitacionesPorPaciente(@PathVariable Long pacienteId) {
        List<DesparasitacionResponseDTO> response = desparasitacionServicio.listarPorPaciente(pacienteId);
        return ResponseEntity.ok(response);
    }
}
