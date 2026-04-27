package com.veterinaria.respositorios;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.Usuario;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
    boolean existsByDni(String dni);

    // Conteo de clientes activos para el dashboard
    long countByActivoTrue();

    Page<Cliente> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContaining(
            String nombre, String apellido, String dni, Pageable pageable);

    Optional<Cliente> findByUsuario(Usuario usuario);
}
