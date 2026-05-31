package com.veterinaria.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OlvidePasswordRequestDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de correo no es válido")
    private String email;
}
