package com.veterinaria.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dtos.ClienteRapidoRequestDTO;
import com.veterinaria.dtos.ClienteRapidoResponseDTO;
import com.veterinaria.dtos.ClienteRequestDTO;
import com.veterinaria.dtos.ClienteResponseDTO;
import com.veterinaria.dtos.CitaResponseDTO;
import com.veterinaria.dtos.DesparasitacionResponseDTO;
import com.veterinaria.dtos.PacienteResponseDTO;
import com.veterinaria.dtos.VacunaResponseDTO;
import com.veterinaria.servicios.ClienteServicio;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteController {

    @Autowired
    private ClienteServicio clienteServicio;

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO respuesta = clienteServicio.guardar(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // =========================================================
    // CLIENTE RÁPIDO: Crea usuario mínimo + cliente + mascotas
    // =========================================================
    @PostMapping("/rapido")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ClienteRapidoResponseDTO> crearClienteRapido(
            @Valid @RequestBody ClienteRapidoRequestDTO dto) {
        ClienteRapidoResponseDTO respuesta = clienteServicio.crearClienteRapido(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listarClientes(
            Pageable pageable,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean estado) {

        Page<ClienteResponseDTO> clientes = clienteServicio.listarTodos(buscar, estado, pageable);

        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<com.veterinaria.dtos.ClienteDashboardDTO> obtenerMiPerfil() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.veterinaria.dtos.ClienteDashboardDTO respuesta = clienteServicio.obtenerDashboardPorEmail(email);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mis-mascotas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PacienteResponseDTO>> listarMisMascotas() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(clienteServicio.listarMisMascotas(email));
    }

    @GetMapping("/mis-mascotas/{pacienteId}/citas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<CitaResponseDTO>> listarCitasDeMiMascota(
            @PathVariable Long pacienteId,
            Pageable pageable) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(clienteServicio.listarCitasDeMiMascota(email, pacienteId, pageable));
    }

    @GetMapping("/mis-mascotas/{pacienteId}/vacunas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<VacunaResponseDTO>> listarVacunasDeMiMascota(@PathVariable Long pacienteId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(clienteServicio.listarVacunasDeMiMascota(email, pacienteId));
    }

    @GetMapping("/mis-mascotas/{pacienteId}/desparasitaciones")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DesparasitacionResponseDTO>> listarDesparasitacionesDeMiMascota(@PathVariable Long pacienteId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(clienteServicio.listarDesparasitacionesDeMiMascota(email, pacienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorId(@PathVariable Long id) {
        ClienteResponseDTO cliente = clienteServicio.buscarPorId(id);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(@PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO clienteActualizado = clienteServicio.actualizar(id, dto);
        return ResponseEntity.ok(clienteActualizado);

    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarEstadoCliente(@PathVariable Long id, @RequestParam Boolean activo) {
        clienteServicio.cambiarEstado(id, activo);
        return ResponseEntity.noContent().build();
    }

}
