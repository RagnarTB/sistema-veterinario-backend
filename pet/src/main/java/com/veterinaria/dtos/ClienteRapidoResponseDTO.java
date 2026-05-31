package com.veterinaria.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteRapidoResponseDTO {

    private Long clienteId;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private Boolean esInvitado;
    private List<PacienteResumenDTO> pacientes;
}
