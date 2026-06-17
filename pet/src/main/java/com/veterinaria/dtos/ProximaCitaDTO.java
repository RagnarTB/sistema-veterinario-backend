package com.veterinaria.dtos;

import lombok.Data;

@Data
public class ProximaCitaDTO {
    private String mascotaNombre;
    private String fotoMascota;
    private String tipoServicio;
    private String doctorNombre;
    private String fechaFormateada;
    private String horaFormateada;
}
