package com.veterinaria.controladores;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.veterinaria.dtos.PacienteRequestDTO;
import com.veterinaria.dtos.PacienteResponseDTO;
import com.veterinaria.modelos.*;
import com.veterinaria.respositorios.PacienteRepositorio;
import com.veterinaria.servicios.PacienteServicio;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    @Autowired
    private PacienteServicio pacienteServicio;

    @PostMapping
    public ResponseEntity<PacienteResponseDTO> crearPaciente(@Valid @RequestBody PacienteRequestDTO dto) {
        // 1. Mandamos a guardar y recibimos el objeto con su nuevo ID
        PacienteResponseDTO respuesta = pacienteServicio.guardar(dto);
        // 2. .body(respuesta) significa "Mete este objeto dentro del paquete y
        // envíaselo a Angular"
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<Page<PacienteResponseDTO>> listarPacientes(Pageable pageable) {
        // 1. Le pedimos la lista ya procesada (DTOs) al servicio
        Page<PacienteResponseDTO> pacientes = pacienteServicio.listarTodos(pageable);

        // 2. La devolvemos envuelta en nuestro ticket de éxito (200 OK)
        return ResponseEntity.ok(pacientes);
        // Nota: ResponseEntity.ok(...) es un atajo elegante para
        // ResponseEntity.status(HttpStatus.OK).body(...)
    }

    @GetMapping("/{id}") // Le decimos que espere un ID en la URL
    public ResponseEntity<PacienteResponseDTO> obtenerPacientePorId(@PathVariable Long id) {

        // Llamamos al servicio pasándole el ID que atrapamos de la URL
        PacienteResponseDTO paciente = pacienteServicio.buscarPorId(id);

        return ResponseEntity.ok(paciente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDTO dto) {

        // Le pasamos al servicio TANTO el ID que queremos buscar, COMO los datos nuevos
        PacienteResponseDTO pacienteActualizado = pacienteServicio.actualizar(id, dto);

        return ResponseEntity.ok(pacienteActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) { // ¡No olvides el @PathVariable!

        // El controlador le delega el trabajo pesado al servicio
        pacienteServicio.eliminar(id);

        // Si todo sale bien, devuelve 204 No Content
        return ResponseEntity.noContent().build();
    }

}