package com.veterinaria.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PacienteResponseDTO {
    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private Long clienteId;

}
