package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class HistorialHospitalizacionRequestDTO {
    @NotNull(message = "El ID de la hospitalización es obligatorio")
    private Long hospitalizacionId;

    @NotNull(message = "El empleado que registra es obligatorio")
    private Long empleadoId;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotBlank(message = "El tipo de acción es obligatorio")
    private String tipoAccion; // Ej. MONITOREO

    // Opcionales (medicación)
    private Long productoId;
    private BigDecimal cantidadAplicada;
    private String origenMedicamento; // CLINICA, CLIENTE, NINGUNO
}
