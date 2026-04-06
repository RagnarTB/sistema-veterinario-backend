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
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<VacunaResponseDTO> registrarVacuna(@Valid @RequestBody VacunaRequestDTO requestDTO) {
        VacunaResponseDTO response = vacunaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<List<VacunaResponseDTO>> listarVacunasPorPaciente(@PathVariable Long pacienteId) {
        List<VacunaResponseDTO> response = vacunaServicio.listarPorPaciente(pacienteId);
        return ResponseEntity.ok(response);
    }
}
