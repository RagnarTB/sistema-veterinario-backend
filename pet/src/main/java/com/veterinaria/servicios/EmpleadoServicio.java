package com.veterinaria.servicios;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.EmpleadoRequestDTO;
import com.veterinaria.dtos.EmpleadoResponseDTO;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Rol;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.EmpleadoRepositorio;
import com.veterinaria.respositorios.RolRespositorio;
import com.veterinaria.respositorios.UsuarioRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;

@Service
public class EmpleadoServicio {

    private final EmpleadoRepositorio empleadoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRespositorio rolRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final SedeRepositorio sedeRepositorio;

    public EmpleadoServicio(EmpleadoRepositorio empleadoRepositorio, UsuarioRepositorio usuarioRepositorio,
            RolRespositorio rolRepositorio, PasswordEncoder passwordEncoder, SedeRepositorio sedeRepositorio) {
        this.empleadoRepositorio = empleadoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.sedeRepositorio = sedeRepositorio;
    }

    @Transactional
    public EmpleadoResponseDTO guardar(EmpleadoRequestDTO dto) {
        if (empleadoRepositorio.existsByDni(dto.getDni())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un empleado con este DNI");
        }
        if (usuarioRepositorio.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un usuario con este email");
        }

        // 1. Crear el usuario para el inicio de sesión
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        Set<Rol> rolesAsignados = new HashSet<>();
        for (String nombreRol : dto.getRoles()) {
            Rol rol = rolRepositorio.findByNombre(nombreRol)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado: " + nombreRol));
            rolesAsignados.add(rol);
        }
        usuario.setRoles(rolesAsignados);
        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);

        // 2. Crear el perfil del empleado
        List<Sede> sedesLista = sedeRepositorio.findAllById(dto.getSedeIds());
        if (sedesLista.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sedes no encontradas");
        }
        Set<Sede> sedes = new HashSet<>(sedesLista);

        Empleado empleado = new Empleado();
        empleado.setNombre(dto.getNombre());
        empleado.setApellido(dto.getApellido());
        empleado.setDni(dto.getDni());
        empleado.setTelefono(dto.getTelefono());
        empleado.setEspecialidad(dto.getEspecialidad());
        empleado.setSueldoBase(dto.getSueldoBase());
        empleado.setSedes(sedes);
        empleado.setUsuario(usuarioGuardado);

        Empleado empleadoGuardado = empleadoRepositorio.save(empleado);

        return mapearAResponse(empleadoGuardado);
    }

    public org.springframework.data.domain.Page<EmpleadoResponseDTO> listarTodos(String buscar, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Empleado> pagina;
        if (buscar != null && !buscar.trim().isEmpty()) {
            pagina = empleadoRepositorio.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContaining(
                    buscar, buscar, buscar, pageable);
        } else {
            pagina = empleadoRepositorio.findAll(pageable);
        }
        return pagina.map(this::mapearAResponse);
    }

    public EmpleadoResponseDTO buscarPorId(Long id) {
        return empleadoRepositorio.findById(id)
                .map(this::mapearAResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));
    }

    @Transactional
    public EmpleadoResponseDTO actualizar(Long id, EmpleadoRequestDTO dto) {
        Empleado empleado = empleadoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));

        // Validar DNI si cambia
        if (!empleado.getDni().equals(dto.getDni()) && empleadoRepositorio.existsByDni(dto.getDni())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro empleado con este DNI");
        }

        Usuario usuario = empleado.getUsuario();
        // Validar Email si cambia
        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepositorio.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro usuario con este email");
        }

        // Actualizar datos del Usuario y Roles
        usuario.setEmail(dto.getEmail());
        Set<Rol> rolesAsignados = new HashSet<>();
        for (String nombreRol : dto.getRoles()) {
            Rol rol = rolRepositorio.findByNombre(nombreRol)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado: " + nombreRol));
            rolesAsignados.add(rol);
        }
        usuario.setRoles(rolesAsignados);
        // Nota: el password generalente se maneja en otro endpoint para cambiar contraseña, se ignora en actualización general de perfil
        usuarioRepositorio.save(usuario);

        List<Sede> sedesLista = sedeRepositorio.findAllById(dto.getSedeIds());
        if (sedesLista.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sedes no encontradas");
        }
        Set<Sede> sedes = new HashSet<>(sedesLista);

        // Actualizar datos del Empleado
        empleado.setNombre(dto.getNombre());
        empleado.setApellido(dto.getApellido());
        empleado.setDni(dto.getDni());
        empleado.setTelefono(dto.getTelefono());
        empleado.setEspecialidad(dto.getEspecialidad());
        empleado.setSueldoBase(dto.getSueldoBase());
        empleado.setSedes(sedes);

        Empleado empleadoGuardado = empleadoRepositorio.save(empleado);
        return mapearAResponse(empleadoGuardado);
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean estado) {
        Empleado empleado = empleadoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));
        
        empleado.setActivo(estado);
        empleadoRepositorio.save(empleado);
        
        // Bloquear también el usuario del sistema si el empleado se inactiva ("despedido")
        Usuario usuario = empleado.getUsuario();
        if (usuario != null) {
            usuario.setActivo(estado);
            usuarioRepositorio.save(usuario);
        }
    }

    private EmpleadoResponseDTO mapearAResponse(Empleado empleado) {
        Set<String> rolesNombres = empleado.getUsuario().getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        Set<Long> sedeIds = empleado.getSedes().stream().map(Sede::getId).collect(Collectors.toSet());
        Set<String> sedeNombres = empleado.getSedes().stream().map(Sede::getNombre).collect(Collectors.toSet());

        return new EmpleadoResponseDTO(
                empleado.getId(),
                empleado.getUsuario().getId(),
                empleado.getUsuario().getEmail(),
                rolesNombres,
                empleado.getNombre(),
                empleado.getApellido(),
                empleado.getDni(),
                empleado.getTelefono(),
                empleado.getEspecialidad(),
                empleado.getSueldoBase(),
                empleado.getActivo(),
                sedeIds,
                sedeNombres
        );
    }
}
