package com.veterinaria.dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRequestDTO {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 150, message = "El apellido no puede superar 150 caracteres")
    private String apellido;

    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    private String telefono;
    @NotBlank(message = "El dni es obligatorio")
    @Size(max = 30, message = "El DNI no puede superar 30 caracteres")
    private String dni;
    @Email(message = "Debe ser un correo válido")
    @NotBlank(message = "El email es obligatorio")
    @Size(max = 254, message = "El email no puede superar 254 caracteres")
    private String email;

}
