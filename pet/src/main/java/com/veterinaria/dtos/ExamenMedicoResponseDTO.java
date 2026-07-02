package com.veterinaria.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExamenMedicoResponseDTO {
    private Long id;
    private String tipoExamen;
    private String resultados;
    private LocalDate fechaSolicitud;
    private Long atencionMedicaId;
}
