package com.veterinaria.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PacienteResumenDTO {
    private Long id;
    private String nombre;
    private String especieNombre;
    private String sexo;
    private Long clienteId;
    private String clienteNombre;
}
