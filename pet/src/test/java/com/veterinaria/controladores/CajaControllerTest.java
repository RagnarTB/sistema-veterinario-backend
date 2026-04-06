package com.veterinaria.controladores;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Sede;
import com.veterinaria.servicios.CajaServicio;
import com.veterinaria.servicios.EmpleadoAutenticadoService;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CajaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CajaServicio cajaServicio;

    @MockBean
    private EmpleadoAutenticadoService empleadoAutenticadoService;

    private Empleado empleadoMock() {
        Sede sede = new Sede();
        sede.setId(1L);
        Empleado empleado = new Empleado();
        empleado.setSedes(new HashSet<>(Set.of(sede)));
        return empleado;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void debeAbrirCajaConSaldoInicial() throws Exception {
        when(empleadoAutenticadoService.obtenerEmpleadoActual()).thenReturn(empleadoMock());
        doNothing().when(cajaServicio).abrirCaja(any(), any());

        String jsonRequest = """
                {
                    "sedeId": 1,
                    "saldoInicial": 100.50
                }
                """;

        mockMvc.perform(post("/api/caja/abrir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void debeCerrarCajaYRetornarEstadoOk() throws Exception {
        when(empleadoAutenticadoService.obtenerEmpleadoActual()).thenReturn(empleadoMock());
        when(cajaServicio.cerrarCaja(any(), any())).thenReturn(null);

        mockMvc.perform(put("/api/caja/cerrar")
                .param("sedeId", "1"))
                .andExpect(status().isOk());
    }
}