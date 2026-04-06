package com.veterinaria.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VacunaResponseDTO {
    private Long id;
    private String nombreVacuna;
    private LocalDate fechaAplicacion;
    private LocalDate fechaProximaDosis;
    private String observaciones;
    private Long pacienteId;
    private String pacienteNombre;
    private Long empleadoId;
    private String empleadoNombre;
}
