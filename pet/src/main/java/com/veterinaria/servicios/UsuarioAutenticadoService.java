package com.veterinaria.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.UsuarioRepositorio;

@Service
public class UsuarioAutenticadoService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado.");
        }

        String email = auth.getName(); 

        return usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado: " + email));
    }

    public Empleado obtenerEmpleadoActual() {
        Usuario usuario = obtenerUsuarioActual();
        Empleado empleado = usuario.getEmpleado();
        if (empleado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario autenticado no tiene un empleado asociado.");
        }
        return empleado;
    }

    public Cliente obtenerClienteActual() {
        Usuario usuario = obtenerUsuarioActual();
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario autenticado no es un cliente.");
        }
        return cliente;
    }

    public boolean tieneRol(String rolNombre) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        
        return auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals(rolNombre));
    }
    
    public boolean esCliente() {
        return tieneRol("ROLE_CLIENTE") && !esAdministradorOVeterinarioORecepcionista();
    }
    
    public boolean esAdministradorOVeterinarioORecepcionista() {
        return tieneRol("ROLE_ADMIN") || tieneRol("ROLE_VETERINARIO") || tieneRol("ROLE_RECEPCIONISTA");
    }
}
