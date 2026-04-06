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
@PreAuthorize("hasRole('ADMIN')")
public class JaulaController {

    private final JaulaServicio jaulaServicio;

    @PostMapping
    public ResponseEntity<JaulaResponseDTO> registrarJaula(@Valid @RequestBody JaulaRequestDTO requestDTO) {
        JaulaResponseDTO response = jaulaServicio.guardar(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JaulaResponseDTO> actualizarJaula(@PathVariable Long id, @Valid @RequestBody JaulaRequestDTO requestDTO) {
        JaulaResponseDTO response = jaulaServicio.actualizar(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<JaulaResponseDTO>> listarJaulas() {
        return ResponseEntity.ok(jaulaServicio.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JaulaResponseDTO> obtenerJaulaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jaulaServicio.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJaula(@PathVariable Long id) {
        jaulaServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
