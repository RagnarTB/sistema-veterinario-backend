package com.veterinaria.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MascotaRapidaDTO {

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotNull(message = "El ID de la especie es obligatorio")
    @Positive(message = "El ID de la especie debe ser positivo")
    private Long especieId;

    @Size(max = 150, message = "La raza no puede superar 150 caracteres")
    private String raza;

    @Size(max = 20, message = "El sexo no puede superar 20 caracteres")
    private String sexo = "MACHO";

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser en el futuro")
    private LocalDate fechaNacimiento;
}
