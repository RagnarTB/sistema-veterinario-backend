package com.veterinaria.controladores;

import com.veterinaria.modelos.RangoPesoJaula;
import com.veterinaria.respositorios.RangoPesoJaulaRepositorio;
import com.veterinaria.respositorios.TamanoJaulaRepositorio;
import com.veterinaria.respositorios.EspecieRepositorio;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rangos-peso-jaula")
@RequiredArgsConstructor
public class RangoPesoJaulaController {
    private final RangoPesoJaulaRepositorio repo;
    private final TamanoJaulaRepositorio tamanoRepo;
    private final EspecieRepositorio especieRepo;

    @Data
    public static class RangoPesoDTO {
        private Long id;
        private EspecieSimpleDTO especie;
        private BigDecimal pesoMinimo;
        private BigDecimal pesoMaximo;
        private TamanoSimpleDTO tamanoJaula;
    }

    @Data
    public static class EspecieSimpleDTO {
        private Long id;
        private String nombre;
    }

    @Data
    public static class TamanoSimpleDTO {
        private Long id;
        private String nombre;
    }

    private RangoPesoDTO toDTO(RangoPesoJaula entidad) {
        RangoPesoDTO dto = new RangoPesoDTO();
        dto.setId(entidad.getId());
        dto.setPesoMinimo(entidad.getPesoMinimo());
        dto.setPesoMaximo(entidad.getPesoMaximo());

        if (entidad.getEspecie() != null) {
            EspecieSimpleDTO esp = new EspecieSimpleDTO();
            esp.setId(entidad.getEspecie().getId());
            esp.setNombre(entidad.getEspecie().getNombre());
            dto.setEspecie(esp);
        }

        if (entidad.getTamanoJaula() != null) {
            TamanoSimpleDTO tam = new TamanoSimpleDTO();
            tam.setId(entidad.getTamanoJaula().getId());
            tam.setNombre(entidad.getTamanoJaula().getNombre());
            dto.setTamanoJaula(tam);
        }

        return dto;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public List<RangoPesoDTO> listarTodas() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RangoPesoDTO crear(@RequestBody RangoPesoJaula entidad) {
        // Resolver las relaciones LAZY correctamente
        if (entidad.getEspecie() != null && entidad.getEspecie().getId() != null) {
            entidad.setEspecie(especieRepo.findById(entidad.getEspecie().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Especie no encontrada")));
        }
        if (entidad.getTamanoJaula() != null && entidad.getTamanoJaula().getId() != null) {
            entidad.setTamanoJaula(tamanoRepo.findById(entidad.getTamanoJaula().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Tamano no encontrado")));
        }
        return toDTO(repo.save(entidad));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RangoPesoDTO> actualizar(@PathVariable Long id, @RequestBody RangoPesoJaula entidad) {
        return repo.findById(id).map(existente -> {
            if (entidad.getEspecie() != null && entidad.getEspecie().getId() != null) {
                existente.setEspecie(especieRepo.findById(entidad.getEspecie().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Especie no encontrada")));
            }
            existente.setPesoMinimo(entidad.getPesoMinimo());
            existente.setPesoMaximo(entidad.getPesoMaximo());
            if (entidad.getTamanoJaula() != null && entidad.getTamanoJaula().getId() != null) {
                existente.setTamanoJaula(tamanoRepo.findById(entidad.getTamanoJaula().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Tamano no encontrado")));
            }
            return ResponseEntity.ok(toDTO(repo.save(existente)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No se puede eliminar el rango de peso.");
        }
    }
}