package com.veterinaria.controladores;

import com.veterinaria.modelos.TamanoJaula;
import com.veterinaria.respositorios.TamanoJaulaRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/tamanos-jaula")
@RequiredArgsConstructor
public class TamanoJaulaController {
    private final TamanoJaulaRepositorio repo;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public List<TamanoJaula> listarTodas() {
        return repo.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TamanoJaula crear(@RequestBody TamanoJaula entidad) {
        return repo.save(entidad);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TamanoJaula> actualizar(@PathVariable Long id, @RequestBody TamanoJaula entidad) {
        return repo.findById(id).map(existente -> {
            existente.setNombre(entidad.getNombre());
            existente.setActivo(entidad.getActivo());
            return ResponseEntity.ok(repo.save(existente));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new com.veterinaria.dtos.MensajeResponseDTO("No se puede eliminar el tamaño porque hay jaulas asignadas a él."));
        }
    }
}