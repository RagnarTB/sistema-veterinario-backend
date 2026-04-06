package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DesparasitacionRequestDTO {

    @NotBlank(message = "El tipo de desparasitación es obligatorio")
    @Size(max = 255, message = "El tipo no puede superar 255 caracteres")
    private String tipo;

    @NotBlank(message = "El producto utilizado es obligatorio")
    @Size(max = 255, message = "El producto utilizado no puede superar 255 caracteres")
    private String productoUtilizado;

    @NotNull(message = "El peso al momento es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private BigDecimal pesoAlMomento;

    @NotNull(message = "La fecha de aplicación es obligatoria")
    @PastOrPresent(message = "La fecha de aplicación no puede ser en el futuro")
    private LocalDate fechaAplicacion;

    @FutureOrPresent(message = "La próxima dosis no puede ser en el pasado")
    private LocalDate fechaProximaDosis;

    @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
    private String observaciones;

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser positivo")
    private Long pacienteId;

    @NotNull(message = "El ID del empleado es obligatorio")
    @Positive(message = "El ID del empleado debe ser positivo")
    private Long empleadoId;
}
