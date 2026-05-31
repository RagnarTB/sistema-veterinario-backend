package com.veterinaria.controladores;

import com.veterinaria.modelos.CategoriaJaula;
import com.veterinaria.respositorios.CategoriaJaulaRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-jaula")
@RequiredArgsConstructor
public class CategoriaJaulaController {
    private final CategoriaJaulaRepositorio repo;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public List<CategoriaJaula> listarTodas() {
        return repo.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaJaula crear(@RequestBody CategoriaJaula entidad) {
        return repo.save(entidad);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaJaula> actualizar(@PathVariable Long id, @RequestBody CategoriaJaula entidad) {
        return repo.findById(id).map(existente -> {
            existente.setNombre(entidad.getNombre());
            existente.setDescripcion(entidad.getDescripcion());
            existente.setActivo(entidad.getActivo());
            existente.setPrecioPorDia(entidad.getPrecioPorDia());
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new com.veterinaria.dtos.MensajeResponseDTO("No se puede eliminar la categoría porque hay jaulas asignadas a ella."));
        }
    }
}