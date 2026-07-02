package com.veterinaria.servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.RolRequestDTO;
import com.veterinaria.dtos.RolResponseDTO;
import com.veterinaria.modelos.Rol;
import com.veterinaria.modelos.Permiso;
import com.veterinaria.respositorios.RolRespositorio;
import com.veterinaria.respositorios.UsuarioRepositorio;
import com.veterinaria.respositorios.PermisoRepositorio;

@Service
public class RolServicio {

    private final RolRespositorio rolRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PermisoRepositorio permisoRepositorio;

    public RolServicio(RolRespositorio rolRepositorio, UsuarioRepositorio usuarioRepositorio, PermisoRepositorio permisoRepositorio) {
        this.rolRepositorio = rolRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.permisoRepositorio = permisoRepositorio;
    }

    public List<RolResponseDTO> listarTodos() {
        return rolRepositorio.findAll().stream()
                .map(rol -> new RolResponseDTO(
                    rol.getId(), 
                    rol.getNombre(), 
                    rol.getActivo(),
                    rol.getPermisos() != null ? rol.getPermisos().stream()
                        .map(p -> new com.veterinaria.dtos.PermisoDTO(p.getNombre(), p.getDescripcion()))
                        .collect(Collectors.toList()) : new java.util.ArrayList<>()
                ))
                .collect(Collectors.toList());
    }

    public RolResponseDTO guardar(RolRequestDTO dto) {
        String nombre = dto.getNombre().toUpperCase().trim();
        if (!nombre.startsWith("ROLE_")) {
            nombre = "ROLE_" + nombre;
        }

        if (rolRepositorio.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol ya existe");
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setActivo(true);
        rol.setFechaModificacionPermisos(java.time.LocalDateTime.now());
        Rol guardado = rolRepositorio.save(rol);

        return new RolResponseDTO(
            guardado.getId(), 
            guardado.getNombre(), 
            guardado.getActivo(), 
            new java.util.ArrayList<>()
        );
    }

    public void cambiarEstado(Long id, Boolean estado) {
        Rol rol = rolRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
                
        List<String> rolesProtegidos = List.of("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
        if (rolesProtegidos.contains(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede cambiar el estado de un rol protegido del sistema");
        }

        if (!estado && usuarioRepositorio.existsByRoles_Id(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No se puede deshabilitar este rol porque actualmente está asignado a empleados.");
        }

        rol.setActivo(estado);
        rolRepositorio.save(rol);
    }

    public void eliminar(Long id) {
        Rol rol = rolRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        List<String> rolesProtegidos = List.of("ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
        if (rolesProtegidos.contains(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar un rol protegido del sistema");
        }

        if (usuarioRepositorio.existsByRoles_Id(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar el rol porque tiene usuarios asignados");
        }

        rolRepositorio.delete(rol);
    }

    public RolResponseDTO actualizarPermisos(Long id, List<String> permisosNombres) {
        Rol rol = rolRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        
        java.util.Set<Permiso> permisosAAsignar = new java.util.HashSet<>();
        if (permisosNombres != null && !permisosNombres.isEmpty()) {
            java.util.List<Permiso> todos = permisoRepositorio.findAll();
            for (Permiso p : todos) {
                if (permisosNombres.contains(p.getNombre())) {
                    permisosAAsignar.add(p);
                }
            }
        }
        
        rol.setPermisos(permisosAAsignar);
        rol.setFechaModificacionPermisos(java.time.LocalDateTime.now()); // Para invalidar tokens antiguos
        
        Rol guardado = rolRepositorio.save(rol);
        
        return new RolResponseDTO(
            guardado.getId(), 
            guardado.getNombre(), 
            guardado.getActivo(),
            guardado.getPermisos().stream()
                .map(p -> new com.veterinaria.dtos.PermisoDTO(p.getNombre(), p.getDescripcion()))
                .collect(Collectors.toList())
        );
    }
}
