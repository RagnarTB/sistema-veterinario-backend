package com.veterinaria.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dtos.AuthResponseDTO;
import com.veterinaria.dtos.LoginRequestDTO;
import com.veterinaria.dtos.MensajeResponseDTO;
import com.veterinaria.dtos.RegistroClienteDTO;
import com.veterinaria.servicios.AuthServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth") // AQUI: Ruta base corregida
public class AuthController {

    @Autowired
    private AuthServicio authServicio;

    @PostMapping("/registro") // AQUI: Endpoint específico
    public ResponseEntity<MensajeResponseDTO> registrarCliente(@Valid @RequestBody RegistroClienteDTO dto) {
        MensajeResponseDTO respuesta = authServicio.registrarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        AuthResponseDTO respuesta = authServicio.login(dto);
        return ResponseEntity.ok(respuesta);

    }
}