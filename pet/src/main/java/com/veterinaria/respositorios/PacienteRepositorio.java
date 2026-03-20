package com.veterinaria.respositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.modelos.Paciente;

public interface PacienteRepositorio extends JpaRepository<Paciente, Long> {

}
