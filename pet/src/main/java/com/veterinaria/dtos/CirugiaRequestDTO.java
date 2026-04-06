package com.veterinaria.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CirugiaRequestDTO {

    @NotBlank(message = "El tipo de cirugía es obligatorio")
    @Size(max = 255, message = "El tipo de cirugía no puede superar 255 caracteres")
    private String tipoCirugia;

    @NotNull(message = "La fecha y hora están obligadas")
    @Future(message = "La cirugía debe programarse para una fecha futura")
    private LocalDateTime fechaHoraFijada;

    @NotBlank(message = "El riesgo operatorio es obligatorio")
    @Size(max = 100, message = "El riesgo operatorio no puede superar 100 caracteres")
    private String riesgoOperatorio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 50, message = "El estado no puede superar 50 caracteres")
    private String estado;

    @Size(max = 2000, message = "Las notas post-operatorias no pueden superar 2000 caracteres")
    private String notasPostOperatorias;

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser positivo")
    private Long pacienteId;

    @NotNull(message = "El ID del cirujano es obligatorio")
    @Positive(message = "El ID del cirujano debe ser positivo")
    private Long cirujanoId;

    @Positive(message = "El ID de hospitalización debe ser positivo")
    private Long hospitalizacionId;
}
