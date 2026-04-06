package com.veterinaria.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsentimientoResponseDTO {
    private Long id;
    private LocalDateTime fechaEmision;
    private String textoLegal;
    private Boolean aceptadoPorCliente;
    private Long cirugiaId;
    private Long clienteId;
    private String clienteNombre;
}
