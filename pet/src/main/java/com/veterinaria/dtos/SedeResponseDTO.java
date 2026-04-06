package com.veterinaria.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SedeResponseDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;
    private Boolean activo;
}
