package com.veterinaria.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SugerenciaJaulaResponseDTO {
    private Long pacienteId;
    private String pacienteNombre;
    private String especieNombre;
    private BigDecimal ultimoPeso;
    
    private Long tamanoSugeridoId;
    private String tamanoSugeridoNombre;

    private List<JaulaResponseDTO> jaulasDisponibles;
}
