package com.veterinaria.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.veterinaria.modelos.Enums.EstadoCita;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CitaResponseDTO {
    private Long id;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String servicioNombre;
    private Long servicioId;
    private Long veterinarioId;
    private String veterinarioNombre;
    private String veterinarioEmail;
    private String motivo;
    private EstadoCita estado;
    private List<Long> pacienteIds;
    private Long sedeId;
    private String sedeNombre;
    private List<PacienteResumenDTO> pacientes;
}