
package com.veterinaria.dtos;

import java.util.List;
import lombok.Data;

@Data
public class ClienteDashboardDTO {
    private String nombreCliente;
    private ProximaCitaDTO proximaCita;
    private List<MascotaDashboardDTO> mascotas;
}
