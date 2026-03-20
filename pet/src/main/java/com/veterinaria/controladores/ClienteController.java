package com.veterinaria.controladores;

import java.util.List;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dtos.ClienteRequestDTO;
import com.veterinaria.dtos.ClienteResponseDTO;
import com.veterinaria.dtos.PacienteResponseDTO;
import com.veterinaria.servicios.ClienteServicio;
import com.veterinaria.servicios.PacienteServicio;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteServicio clienteServicio;

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO respuesta = clienteServicio.guardar(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping()
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes() {
        List<ClienteResponseDTO> clientes = clienteServicio.listarTodos();

        return ResponseEntity.ok(clientes);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
