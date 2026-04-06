package com.veterinaria.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HospitalizacionResponseDTO {
    private Long id;
    private String motivoIngreso;
    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaAlta;
    private String estado;
    private Long pacienteId;
    private String pacienteNombre;
    private Long jaulaId;
    private String jaulaNumero;
    private Long empleadoId;
    private String empleadoNombre;
}
