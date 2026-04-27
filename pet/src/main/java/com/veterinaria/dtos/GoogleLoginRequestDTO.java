package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDTO {

    @NotBlank(message = "El idToken es obligatorio")
    private String idToken;
}
