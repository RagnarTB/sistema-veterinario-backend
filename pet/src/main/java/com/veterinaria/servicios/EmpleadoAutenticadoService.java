package com.veterinaria.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.UsuarioRepositorio;

@Service
public class EmpleadoAutenticadoService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public Empleado obtenerEmpleadoActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado.");
        }

        String email = auth.getName(); // el username que pusiste en JwtServicio

        Usuario usuario = usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado: " + email));

        Empleado empleado = usuario.getEmpleado();
        if (empleado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario autenticado no tiene un empleado asociado.");
        }

        return empleado;
    }
}
