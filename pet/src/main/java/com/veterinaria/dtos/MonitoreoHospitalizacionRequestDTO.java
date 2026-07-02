package com.veterinaria.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonitoreoHospitalizacionRequestDTO {

    @NotNull(message = "La fecha y hora es obligatoria")
    private LocalDateTime fechaHora;

    @Positive(message = "La temperatura debe ser un valor positivo")
    private BigDecimal temperatura;

    @Positive(message = "La frecuencia cardiaca debe ser un valor positivo")
    private Integer frecuenciaCardiaca;

    private String apetito;

    private String observaciones;

    @NotNull(message = "El ID de la hospitalización es obligatorio")
    private Long hospitalizacionId;

    @NotNull(message = "El ID del empleado es obligatorio")
    private Long empleadoId;
}
