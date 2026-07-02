package com.veterinaria.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaKardexDTO {
    private Long id;
    private Long pacienteId;
    private LocalDateTime fechaHora;
    private String usuario;
    private String accion;
    private String modulo;
    private String detalle;
    private String motivo;
}
