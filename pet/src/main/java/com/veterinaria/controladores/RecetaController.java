package com.veterinaria.controladores;

import com.veterinaria.dtos.RecetaRequestDTO;
import com.veterinaria.dtos.RecetaResponseDTO;
import com.veterinaria.servicios.RecetaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaServicio recetaServicio;

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_REGISTROS_CLINICOS')")
    public ResponseEntity<RecetaResponseDTO> generarReceta(@Valid @RequestBody RecetaRequestDTO requestDTO) {
        RecetaResponseDTO response = recetaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasAuthority('VER_HISTORIAL')")
    public ResponseEntity<RecetaResponseDTO> obtenerRecetaPorAtencion(@PathVariable Long atencionId) {
        RecetaResponseDTO response = recetaServicio.obtenerPorAtencionMedica(atencionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_HISTORIAL')")
    public ResponseEntity<RecetaResponseDTO> obtenerRecetaPorId(@PathVariable Long id) {
        RecetaResponseDTO response = recetaServicio.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }
}
