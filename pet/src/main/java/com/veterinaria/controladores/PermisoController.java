package com.veterinaria.controladores;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dtos.PermisoDTO;
import com.veterinaria.respositorios.PermisoRepositorio;

@RestController
@RequestMapping("/api/permisos")
@PreAuthorize("hasAuthority('GESTIONAR_ROLES_PERMISOS')")
public class PermisoController {

    private final PermisoRepositorio permisoRepositorio;

    public PermisoController(PermisoRepositorio permisoRepositorio) {
        this.permisoRepositorio = permisoRepositorio;
    }

    @GetMapping
    public List<PermisoDTO> listarTodos() {
        return permisoRepositorio.findAll().stream()
                .map(p -> new PermisoDTO(p.getNombre(), p.getDescripcion()))
                .collect(Collectors.toList());
    }
}
