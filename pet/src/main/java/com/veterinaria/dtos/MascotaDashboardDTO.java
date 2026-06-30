package com.veterinaria.dtos;

import lombok.Data;

@Data
public class MascotaDashboardDTO {
    private Long id;
    private String nombre;
    private String foto;
    private ResumenSaludDTO resumenSalud;
}
