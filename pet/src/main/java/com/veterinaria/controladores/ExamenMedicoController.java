package com.veterinaria.controladores;

import com.veterinaria.dtos.ExamenMedicoRequestDTO;
import com.veterinaria.dtos.ExamenMedicoResponseDTO;
import com.veterinaria.servicios.ExamenMedicoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examenes")
@RequiredArgsConstructor
public class ExamenMedicoController {

    private final ExamenMedicoServicio examenMedicoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<ExamenMedicoResponseDTO> solicitarExamen(@Valid @RequestBody ExamenMedicoRequestDTO requestDTO) {
        ExamenMedicoResponseDTO response = examenMedicoServicio.solicitarExamen(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/resultados")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<ExamenMedicoResponseDTO> actualizarResultados(
            @PathVariable Long id,
            @RequestBody String resultados) {
        ExamenMedicoResponseDTO response = examenMedicoServicio.actualizarResultados(id, resultados);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<List<ExamenMedicoResponseDTO>> listarPorAtencionMedica(@PathVariable Long atencionId) {
        List<ExamenMedicoResponseDTO> response = examenMedicoServicio.listarPorAtencionMedica(atencionId);
        return ResponseEntity.ok(response);
    }
}
