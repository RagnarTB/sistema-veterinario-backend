package com.veterinaria.servicios;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.veterinaria.dtos.ClienteRapidoRequestDTO;
import com.veterinaria.dtos.ClienteRapidoResponseDTO;
import com.veterinaria.dtos.ClienteRequestDTO;
import com.veterinaria.dtos.ClienteResponseDTO;
import com.veterinaria.dtos.MascotaRapidaDTO;
import com.veterinaria.dtos.PacienteResumenDTO;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.Especie;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.respositorios.EspecieRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;

import jakarta.transaction.Transactional;

@Service
public class ClienteServicio {

    private ClienteRepositorio clienteRepositorio;
    private com.veterinaria.respositorios.UsuarioRepositorio usuarioRepositorio;
    private com.veterinaria.respositorios.RolRespositorio rolRespositorio;
    private com.veterinaria.respositorios.VerificationTokenRepositorio tokenRepositorio;
    private EmailServicio emailServicio;
    private final EspecieRepositorio especieRepositorio;
    private final PacienteRepositorio pacienteRepositorio;

    public ClienteServicio(ClienteRepositorio clienteRepositorio, 
                           com.veterinaria.respositorios.UsuarioRepositorio usuarioRepositorio,
                           com.veterinaria.respositorios.RolRespositorio rolRespositorio,
                           com.veterinaria.respositorios.VerificationTokenRepositorio tokenRepositorio, 
                           EmailServicio emailServicio,
                           EspecieRepositorio especieRepositorio,
                           PacienteRepositorio pacienteRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRespositorio = rolRespositorio;
        this.tokenRepositorio = tokenRepositorio;
        this.emailServicio = emailServicio;
        this.especieRepositorio = especieRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
    }

    // =========================================================
    // CREAR CLIENTE RÁPIDO (sin email/password, con mascotas)
    // =========================================================
    @Transactional
    public ClienteRapidoResponseDTO crearClienteRapido(ClienteRapidoRequestDTO dto) {

        // 1. Buscar si ya existe un usuario con ese DNI
        Usuario usuario = usuarioRepositorio.findByDni(dto.getDni()).orElse(null);

        if (usuario != null) {
            // Si ya tiene email y password → es un cliente registrado completo
            if (usuario.getEmail() != null && usuario.getPassword() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este DNI ya pertenece a un cliente registrado. Búsquelo en la lista de clientes.");
            }

            // Si ya existe como invitado → reusar (idempotente)
            Cliente clienteExistente = clienteRepositorio.findByUsuarioId(usuario.getId()).orElse(null);
            if (clienteExistente != null) {
                // Actualizar datos por si cambiaron
                usuario.setNombre(dto.getNombre());
                usuario.setApellido(dto.getApellido());
                if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
                    usuario.setTelefono(dto.getTelefono());
                }
                usuarioRepositorio.save(usuario);

                // Crear las mascotas nuevas (no duplicar existentes)
                List<PacienteResumenDTO> pacientesCreados = crearMascotasParaCliente(dto.getMascotas(), clienteExistente);

                return new ClienteRapidoResponseDTO(
                        clienteExistente.getId(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getDni(),
                        usuario.getTelefono(),
                        clienteExistente.getEsInvitado(),
                        pacientesCreados);
            }
        }

        // 2. Crear Usuario mínimo (sin password)
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setDni(dto.getDni());
            usuario.setNombre(dto.getNombre());
            usuario.setApellido(dto.getApellido());
            usuario.setTelefono(dto.getTelefono() != null ? dto.getTelefono() : "");

            // Asignar email si viene en el request y no está en uso
            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                if (usuarioRepositorio.findByEmail(dto.getEmail()).isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El email proporcionado ya está en uso por otro cliente.");
                }
                usuario.setEmail(dto.getEmail());
            }

            usuario.setDireccion(dto.getDireccion() != null ? dto.getDireccion() : "");

            usuario.setActivo(true);

            // Asignar rol ROLE_CLIENTE
            com.veterinaria.modelos.Rol rolCliente = rolRespositorio.findByNombre("ROLE_CLIENTE")
                    .orElseThrow(() -> new RuntimeException("Rol ROLE_CLIENTE no encontrado"));
            usuario.getRoles().add(rolCliente);
            usuario = usuarioRepositorio.save(usuario);
        }

        // 3. Crear Cliente con esInvitado=true
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setEsInvitado(true);
        cliente.setActivo(true);
        Cliente clienteGuardado = clienteRepositorio.save(cliente);

        // 4. Crear las mascotas
        List<PacienteResumenDTO> pacientesCreados = crearMascotasParaCliente(dto.getMascotas(), clienteGuardado);

        // Si se proporcionó email y el cliente fue creado, enviar token de confirmación
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank() && usuario.getPassword() == null) {
            String token = java.util.UUID.randomUUID().toString();
            com.veterinaria.modelos.VerificationToken verificationToken = new com.veterinaria.modelos.VerificationToken(
                    token, 
                    clienteGuardado, 
                    java.time.LocalDateTime.now().plusDays(1)
            );
            tokenRepositorio.save(verificationToken);
            emailServicio.enviarCorreoConfirmacion(usuario.getEmail(), token);
        }

        return new ClienteRapidoResponseDTO(
                clienteGuardado.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getDni(),
                usuario.getTelefono(),
                true,
                pacientesCreados);
    }

    private List<PacienteResumenDTO> crearMascotasParaCliente(List<MascotaRapidaDTO> mascotas, Cliente cliente) {
        List<PacienteResumenDTO> resultado = new ArrayList<>();
        String nombreCliente = cliente.getUsuario().getNombre() + " " + cliente.getUsuario().getApellido();

        for (MascotaRapidaDTO mascotaDto : mascotas) {
            Especie especie = especieRepositorio.findById(mascotaDto.getEspecieId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Especie no encontrada con ID: " + mascotaDto.getEspecieId()));

            Paciente paciente = new Paciente();
            paciente.setNombre(mascotaDto.getNombre());
            paciente.setEspecie(especie);
            paciente.setRaza(mascotaDto.getRaza());
            paciente.setSexo(mascotaDto.getSexo());
            paciente.setFechaNacimiento(mascotaDto.getFechaNacimiento());
            paciente.setCliente(cliente);
            paciente.setActivo(true);

            Paciente pacienteGuardado = pacienteRepositorio.save(paciente);

            resultado.add(new PacienteResumenDTO(
                    pacienteGuardado.getId(),
                    pacienteGuardado.getNombre(),
                    especie.getNombre(),
                    pacienteGuardado.getSexo(),
                    cliente.getId(),
                    nombreCliente));
        }

        return resultado;
    }

    @Transactional
    public ClienteResponseDTO guardar(ClienteRequestDTO dto) {

        com.veterinaria.modelos.Usuario usuario = usuarioRepositorio.findByDni(dto.getDni()).orElse(null);
        if (usuario != null && usuario.getCliente() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI proporcionado ya está registrado como cliente.");
        }

        if (usuario == null) {
            usuario = new com.veterinaria.modelos.Usuario();
            usuario.setNombre(dto.getNombre());
            usuario.setApellido(dto.getApellido());
            usuario.setTelefono(dto.getTelefono());
            usuario.setDni(dto.getDni());
            usuario.setEmail(dto.getEmail());
            usuario.setDireccion(dto.getDireccion());
            usuario.setActivo(false);
            
            com.veterinaria.modelos.Rol rolCliente = rolRespositorio.findByNombre("ROLE_CLIENTE")
                    .orElseThrow(() -> new RuntimeException("Rol ROLE_CLIENTE no encontrado"));
            usuario.getRoles().add(rolCliente);
            usuario = usuarioRepositorio.save(usuario);
        }

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setActivo(false);
        cliente.setEsInvitado(false);

        Cliente clienteGuardado = clienteRepositorio.save(cliente);

        // Generar Token de Verificacion
        String token = java.util.UUID.randomUUID().toString();
        com.veterinaria.modelos.VerificationToken verificationToken = new com.veterinaria.modelos.VerificationToken(
                token, 
                clienteGuardado, 
                java.time.LocalDateTime.now().plusDays(1)
        );
        tokenRepositorio.save(verificationToken);

        // Enviar Email
        emailServicio.enviarCorreoConfirmacion(usuario.getEmail(), token);

        ClienteResponseDTO respuesta = new ClienteResponseDTO();
        respuesta.setId(clienteGuardado.getId());
        respuesta.setNombre(usuario.getNombre());
        respuesta.setApellido(usuario.getApellido());
        respuesta.setTelefono(usuario.getTelefono());
        respuesta.setDni(usuario.getDni());
        respuesta.setEmail(usuario.getEmail());
        respuesta.setActivo(clienteGuardado.getActivo());
        respuesta.setVerificado(false); // First time creation is never verified

        return respuesta;
    }

    public Page<ClienteResponseDTO> listarTodos(String buscar, Boolean estado, Pageable pageable) {
        Page<Cliente> pagina;
        if (buscar != null && !buscar.trim().isEmpty()) {
            pagina = clienteRepositorio.buscarClientesConRol(buscar, estado, pageable);
        } else {
            pagina = clienteRepositorio.findAllConRol(estado, pageable);
        }

        return pagina.map(cliente -> {
            boolean verificado = cliente.getUsuario() != null && cliente.getUsuario().getPassword() != null;
            com.veterinaria.modelos.Usuario u = cliente.getUsuario();
            return new ClienteResponseDTO(
                cliente.getId(),
                u != null ? u.getNombre() : "",
                u != null ? u.getApellido() : "",
                u != null ? u.getTelefono() : "",
                u != null ? u.getDni() : "",
                u != null ? u.getEmail() : "",
                cliente.getActivo(),
                verificado,
                u != null ? u.getDireccion() : "");
        });
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        return clienteRepositorio.findById(id)
                .map(cliente -> {
                    boolean verificado = cliente.getUsuario() != null && cliente.getUsuario().getPassword() != null;
                    com.veterinaria.modelos.Usuario u = cliente.getUsuario();
                    return new ClienteResponseDTO(
                        cliente.getId(),
                        u != null ? u.getNombre() : "",
                        u != null ? u.getApellido() : "",
                        u != null ? u.getTelefono() : "",
                        u != null ? u.getDni() : "",
                        u != null ? u.getEmail() : "",
                        cliente.getActivo(),
                        verificado,
                        u != null ? u.getDireccion() : "");
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + id));

    }

    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente clientedb = clienteRepositorio.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con ID: " + id));

        com.veterinaria.modelos.Usuario u = clientedb.getUsuario();
        if (u != null) {
            u.setNombre(dto.getNombre());
            u.setApellido(dto.getApellido());
            u.setTelefono(dto.getTelefono());
            u.setDni(dto.getDni());
            u.setDireccion(dto.getDireccion());

            // Lógica para asignar correo si no tenía y disparar confirmación
            if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                if (!dto.getEmail().equals(u.getEmail())) {
                    if (usuarioRepositorio.findByEmail(dto.getEmail()).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está en uso");
                    }
                    u.setEmail(dto.getEmail());
                    
                    // Si el usuario es invitado y no tiene password, le enviamos token
                    if (u.getPassword() == null) {
                        String token = java.util.UUID.randomUUID().toString();
                        com.veterinaria.modelos.VerificationToken verificationToken = new com.veterinaria.modelos.VerificationToken(
                                token, 
                                clientedb, 
                                java.time.LocalDateTime.now().plusDays(1)
                        );
                        tokenRepositorio.save(verificationToken);
                        emailServicio.enviarCorreoConfirmacion(u.getEmail(), token);
                    }
                }
            }
            usuarioRepositorio.save(u);
        }

        Cliente clienteGuardado = clienteRepositorio.save(clientedb);

        boolean verificado = clienteGuardado.getUsuario() != null && clienteGuardado.getUsuario().getPassword() != null;
        return new ClienteResponseDTO(
                clienteGuardado.getId(),
                u != null ? u.getNombre() : "",
                u != null ? u.getApellido() : "",
                u != null ? u.getTelefono() : "",
                u != null ? u.getDni() : "",
                u != null ? u.getEmail() : "",
                clienteGuardado.getActivo(),
                verificado,
                u != null ? u.getDireccion() : ""
        );
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean estado) {
        Cliente clientedb = clienteRepositorio.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con ID:" + id));

        if (estado) {
            // El admin acaba de darle click a "Activar".
            clientedb.setActivo(false); 
            // Si el usuario no tiene contraseña (nuevo), enviamos correo
            if (clientedb.getUsuario() != null && clientedb.getUsuario().getPassword() == null) {
                // Generar nuevo token y reenviar
                String token = java.util.UUID.randomUUID().toString();
                com.veterinaria.modelos.VerificationToken verificationToken = new com.veterinaria.modelos.VerificationToken(
                        token, 
                        clientedb, 
                        java.time.LocalDateTime.now().plusDays(1)
                );
                tokenRepositorio.save(verificationToken);
                String correo = clientedb.getUsuario().getEmail();
                if (correo != null) {
                    emailServicio.enviarCorreoConfirmacion(correo, token);
                }
            } else {
                clientedb.setActivo(true);
            }
        } else {
            // Desactivar inmediatamente el rol de cliente
            clientedb.setActivo(false);
            // Solo desactivamos el LOGIN si NO es empleado
            if (clientedb.getUsuario() != null && clientedb.getUsuario().getEmpleado() == null) {
                clientedb.getUsuario().setActivo(false);
                usuarioRepositorio.save(clientedb.getUsuario());
            }
        }

        if (clientedb.getPacientes() != null) {
            clientedb.getPacientes().forEach(paciente -> paciente.setActivo(estado != null ? estado : false));
        }
        clienteRepositorio.save(clientedb);
    }

}
