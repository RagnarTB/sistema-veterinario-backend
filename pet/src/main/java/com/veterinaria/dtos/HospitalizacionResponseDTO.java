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
    private String nivelGravedad;
    private Integer frecuenciaMonitoreoHoras;
    private LocalDateTime proximoMonitoreo;
    private Boolean monitoreoAtrasado;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteEspecie;
    private Long jaulaId;
    private String jaulaNumero;
    private Long empleadoId;
    private String empleadoNombre;
}
