package com.veterinaria.controladores;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.servicios.AtencionMedicaServicio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // Aquí NO apagamos los filtros, queremos probar la seguridad real
class AtencionMedicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AtencionMedicaServicio atencionMedicaServicio;

    // --- TEST 1: LA RECEPCIONISTA (Debe ser rechazada) ---
    @Test
    @WithMockUser(roles = "RECEPCIONISTA") // Simulamos ser Recepcionista
    void recepcionistaNoDebePoderCrearAtencionMedica() throws Exception {
        String jsonRequest = """
                {
                    "sintomas": "Fiebre y letargo",
                    "diagnostico": "Parvovirus",
                    "tratamiento": "Suero y antibióticos",
                    "peso": 12.5,
                    "temperatura": 39.5,
                    "frecuenciaCardiaca": 110,
                    "citaId": 1
                }
                """;

        // Como es recepcionista, esperamos que el sistema le devuelva 403 FORBIDDEN
        // (Prohibido)
        mockMvc.perform(post("/api/atenciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isForbidden());
    }

    // --- TEST 2: EL VETERINARIO (Debe ser aceptado) ---
    @Test
    @WithMockUser(roles = "VETERINARIO") // Simulamos ser Médico
    void veterinarioSiPuedeCrearAtencionMedica() throws Exception {
        String jsonRequest = """
                {
                    "sintomas": "Fiebre y letargo",
                    "diagnostico": "Parvovirus",
                    "tratamiento": "Suero y antibióticos",
                    "peso": 12.5,
                    "temperatura": 39.5,
                    "frecuenciaCardiaca": 110,
                    "citaId": 1
                }
                """;

        // Como es médico, esperamos que el sistema sí procese la petición y devuelva
        // 201 CREATED
        mockMvc.perform(post("/api/atenciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated());
    }
}