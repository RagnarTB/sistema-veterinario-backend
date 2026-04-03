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
    // EL CAMBIO, Renombramos la variable para ser super especificos
    private String especieNombre;
    private String raza;
    private Long clienteId;

}
