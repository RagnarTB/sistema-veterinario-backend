package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ExamenMedicoRequestDTO {

    @NotBlank(message = "El tipo de examen es obligatorio")
    private String tipoExamen;

    private String resultados;

    @NotNull(message = "La fecha de solicitud es obligatoria")
    private LocalDate fechaSolicitud;

    @NotNull(message = "El ID de la atención médica es obligatorio")
    private Long atencionMedicaId;
}
