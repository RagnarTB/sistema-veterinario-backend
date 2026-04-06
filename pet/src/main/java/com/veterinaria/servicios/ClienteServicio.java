package com.veterinaria.servicios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.veterinaria.dtos.ClienteRequestDTO;
import com.veterinaria.dtos.ClienteResponseDTO;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.respositorios.ClienteRepositorio;

import jakarta.transaction.Transactional;

@Service
public class ClienteServicio {

    private ClienteRepositorio clienteRepositorio;

    public ClienteServicio(ClienteRepositorio clienteRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
    }

    public ClienteResponseDTO guardar(ClienteRequestDTO dto) {

        if (clienteRepositorio.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya se encuentra registrado");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setApellido(dto.getApellido());
        cliente.setTelefono(dto.getTelefono());
        cliente.setDni(dto.getDni());
        cliente.setEmail(dto.getEmail());

        // Como este cliente lo está creando la recepcionista directamente, no tiene
        // usuario web
        cliente.setEsInvitado(true);

        Cliente clienteGuardado = clienteRepositorio.save(cliente);

        ClienteResponseDTO respuesta = new ClienteResponseDTO();
        respuesta.setId(clienteGuardado.getId());
        respuesta.setNombre(clienteGuardado.getNombre());
        respuesta.setApellido(clienteGuardado.getApellido());
        respuesta.setTelefono(clienteGuardado.getTelefono());
        respuesta.setDni(clienteGuardado.getDni());
        respuesta.setEmail(clienteGuardado.getEmail());

        return respuesta;

    }

    public Page<ClienteResponseDTO> listarTodos(String buscar, Pageable pageable) {
        Page<Cliente> pagina;
        if (buscar != null && !buscar.trim().isEmpty()) {
            pagina = clienteRepositorio.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContaining(
                    buscar, buscar, buscar, pageable);
        } else {
            pagina = clienteRepositorio.findAll(pageable);
        }

        return pagina.map(cliente -> new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTelefono(),
                cliente.getDni(),
                cliente.getEmail()));
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        return clienteRepositorio.findById(id)
                .map(cliente -> new ClienteResponseDTO(
                        cliente.getId(),
                        cliente.getNombre(),
                        cliente.getApellido(),
                        cliente.getTelefono(),
                        cliente.getDni(), cliente.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + id));

    }

    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente clientedb = clienteRepositorio.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con ID: " + id));

        clientedb.setNombre(dto.getNombre());
        clientedb.setApellido(dto.getApellido());
        clientedb.setTelefono(dto.getTelefono());
        clientedb.setDni(dto.getDni());
        clientedb.setEmail(dto.getEmail());

        Cliente clienteGuardado = clienteRepositorio.save(clientedb);

        return new ClienteResponseDTO(
                clienteGuardado.getId(),
                clienteGuardado.getNombre(),
                clienteGuardado.getApellido(),
                clienteGuardado.getTelefono(),
                clienteGuardado.getDni(),
                clienteGuardado.getEmail()

        );
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean estado) {
        Cliente clientedb = clienteRepositorio.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con ID:" + id));
        clientedb.setActivo(estado);

        if (clientedb.getPacientes() != null) {
            clientedb.getPacientes().forEach(paciente -> paciente.setActivo(estado));
        }
        clienteRepositorio.save(clientedb);
    }

}
