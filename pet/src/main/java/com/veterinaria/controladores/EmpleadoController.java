package com.veterinaria.controladores;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veterinaria.dtos.EmpleadoRequestDTO;
import com.veterinaria.dtos.EmpleadoResponseDTO;
import com.veterinaria.servicios.EmpleadoServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoServicio empleadoServicio;

    public EmpleadoController(EmpleadoServicio empleadoServicio) {
        this.empleadoServicio = empleadoServicio;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmpleadoResponseDTO guardar(@Valid @RequestBody EmpleadoRequestDTO dto) {
        return empleadoServicio.guardar(dto);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<EmpleadoResponseDTO> listarTodos(
            Pageable pageable,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) Long sedeId) {
        return empleadoServicio.listarTodos(buscar, estado, sedeId, pageable);
    }

    @GetMapping("/veterinarios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmpleadoResponseDTO>> listarVeterinarios() {
        return ResponseEntity.ok(empleadoServicio.listarVeterinarios());
    }

    @GetMapping("/veterinarios/{sedeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmpleadoResponseDTO>> listarVeterinariosPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(empleadoServicio.listarVeterinariosPorSede(sedeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmpleadoResponseDTO buscarPorId(@PathVariable Long id) {
        return empleadoServicio.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    public EmpleadoResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody EmpleadoRequestDTO dto) {
        return empleadoServicio.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('GESTIONAR_EMPLEADOS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(@PathVariable Long id, @RequestParam Boolean estado) {
        empleadoServicio.cambiarEstado(id, estado);
    }
}
