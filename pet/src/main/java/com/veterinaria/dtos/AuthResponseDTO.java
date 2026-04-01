package com.veterinaria.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String token; // Aquí irá el choricito de texto JWT
    private String email;
    // Más adelante devolveremos el ROL aquí también para que Angular sepa qué menús
    // mostrar
}