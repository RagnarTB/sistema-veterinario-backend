package com.veterinaria.respositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.modelos.Empleado;

public interface EmpleadoRepositorio extends JpaRepository<Empleado, Long> {

    boolean existsByDni(String dni);

    // Método solicitado para encontrar empleado por el email del usuario
    Optional<Empleado> findByUsuarioEmail(String email);
    
    // Método para evitar que el mismo usuario se asigne a más de un empleado
    boolean existsByUsuarioId(Long usuarioId);
}
