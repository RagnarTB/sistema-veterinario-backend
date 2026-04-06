package com.veterinaria.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CirugiaResponseDTO {
    private Long id;
    private String tipoCirugia;
    private LocalDateTime fechaHoraFijada;
    private String riesgoOperatorio;
    private String estado;
    private String notasPostOperatorias;
    private Long pacienteId;
    private String pacienteNombre;
    private Long cirujanoId;
    private String cirujanoNombre;
    private Long hospitalizacionId;
}
