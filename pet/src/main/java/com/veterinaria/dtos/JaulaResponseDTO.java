package com.veterinaria.dtos;

import lombok.Data;

@Data
public class JaulaResponseDTO {
    private Long id;
    private String numero;
    private String tipo;
    private String estado;
    private Long sedeId;
    private String sedeNombre;
}
