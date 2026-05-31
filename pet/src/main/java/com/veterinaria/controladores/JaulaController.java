package com.veterinaria.controladores;

import com.veterinaria.dtos.JaulaRequestDTO;
import com.veterinaria.dtos.JaulaResponseDTO;
import com.veterinaria.servicios.JaulaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jaulas")
@RequiredArgsConstructor
public class JaulaController {

    private final JaulaServicio jaulaServicio;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JaulaResponseDTO> registrarJaula(@Valid @RequestBody JaulaRequestDTO requestDTO) {
        JaulaResponseDTO response = jaulaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JaulaResponseDTO> actualizarJaula(@PathVariable Long id, @Valid @RequestBody JaulaRequestDTO requestDTO) {
        JaulaResponseDTO response = jaulaServicio.actualizar(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<List<JaulaResponseDTO>> listarJaulas() {
        return ResponseEntity.ok(jaulaServicio.listarTodas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<JaulaResponseDTO> obtenerJaulaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jaulaServicio.obtenerPorId(id));
    }

    @GetMapping("/sede/{sedeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<List<JaulaResponseDTO>> listarPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(jaulaServicio.listarPorSede(sedeId));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<JaulaResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(jaulaServicio.cambiarEstado(id, nuevoEstado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarJaula(@PathVariable Long id) {
        jaulaServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
