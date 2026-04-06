package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CirugiaRequestDTO {

    @NotBlank(message = "El tipo de cirugía es obligatorio")
    private String tipoCirugia;

    @NotNull(message = "La fecha y hora están obligadas")
    private LocalDateTime fechaHoraFijada;

    @NotBlank(message = "El riesgo operatorio es obligatorio")
    private String riesgoOperatorio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    private String notasPostOperatorias;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del cirujano es obligatorio")
    private Long cirujanoId;

    private Long hospitalizacionId;
}
