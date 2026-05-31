package com.veterinaria.dtos;

import lombok.Data;

@Data
public class JaulaResponseDTO {
    private Long id;
    private String numero;
    private String categoriaNombre;
    private String tamanoNombre;
    private Long tamanoId;
    private Boolean alertaContagio;
    private Boolean activo;
    private String estado;
    private Long sedeId;
    private String sedeNombre;
}
