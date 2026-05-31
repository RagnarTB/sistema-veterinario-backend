package com.veterinaria.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRapidoRequestDTO {

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 150, message = "El apellido no puede superar 150 caracteres")
    private String apellido;

    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    private String telefono;

    @jakarta.validation.constraints.Email(message = "El formato de correo no es válido")
    @Size(max = 200, message = "El correo no puede superar 200 caracteres")
    private String email;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;

    @NotEmpty(message = "Debe registrar al menos una mascota")
    @Valid
    private List<MascotaRapidaDTO> mascotas;
}
