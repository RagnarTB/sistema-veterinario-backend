package com.veterinaria.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequestDTO {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;
    private String apellido;
    private String telefono;
    @NotBlank(message = "El dni es obligatorio")
    private String dni;

}
