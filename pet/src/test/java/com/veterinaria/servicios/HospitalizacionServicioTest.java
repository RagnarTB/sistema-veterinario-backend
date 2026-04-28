package com.veterinaria.servicios;

import com.veterinaria.dtos.HospitalizacionRequestDTO;
import com.veterinaria.dtos.HospitalizacionResponseDTO;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Hospitalizacion;
import com.veterinaria.modelos.Jaula;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.respositorios.EmpleadoRepositorio;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import com.veterinaria.respositorios.JaulaRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HospitalizacionServicioTest {

    @Mock
    private HospitalizacionRepositorio hospitalizacionRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private JaulaRepositorio jaulaRepositorio;

    @Mock
    private EmpleadoRepositorio empleadoRepositorio;

    @InjectMocks
    private HospitalizacionServicio hospitalizacionServicio;

    private Paciente paciente;
    private Jaula jaula;
    private Empleado veterinario;
    private Hospitalizacion hospitalizacion;
    private HospitalizacionRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Max");

        jaula = new Jaula();
        jaula.setId(1L);
        jaula.setEstado("DISPONIBLE");

        veterinario = new Empleado();
        veterinario.setId(1L);

        hospitalizacion = new Hospitalizacion();
        hospitalizacion.setId(10L);
        hospitalizacion.setEstado("ACTIVA");
        hospitalizacion.setPaciente(paciente);
        hospitalizacion.setJaula(jaula);
        hospitalizacion.setEmpleado(veterinario);

        requestDTO = new HospitalizacionRequestDTO();
        requestDTO.setPacienteId(1L);
        requestDTO.setJaulaId(1L);
        requestDTO.setEmpleadoId(1L);
        requestDTO.setMotivoIngreso("Cirugia");
    }

    @Test
    void ingresarPaciente_Exito() {
        when(hospitalizacionRepositorio.findByPacienteIdAndEstado(1L, "ACTIVA")).thenReturn(Optional.empty());
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));
        when(empleadoRepositorio.findById(1L)).thenReturn(Optional.of(veterinario));
        when(hospitalizacionRepositorio.save(any(Hospitalizacion.class))).thenReturn(hospitalizacion);

        HospitalizacionResponseDTO response = hospitalizacionServicio.ingresarPaciente(requestDTO);

        assertNotNull(response);
        assertEquals("OCUPADA", jaula.getEstado()); // Verify side-effect on jaula
        verify(jaulaRepositorio).save(jaula);
        verify(hospitalizacionRepositorio).save(any(Hospitalizacion.class));
    }

    @Test
    void ingresarPaciente_FallaSiYaTieneHospitalizacionActiva() {
        when(hospitalizacionRepositorio.findByPacienteIdAndEstado(1L, "ACTIVA")).thenReturn(Optional.of(hospitalizacion));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> hospitalizacionServicio.ingresarPaciente(requestDTO));
        assertEquals("El paciente ya se encuentra hospitalizado (Hospitalización ACTIVA).", exception.getMessage());
    }

    @Test
    void ingresarPaciente_FallaSiJaulaNoDisponible() {
        jaula.setEstado("OCUPADA");
        when(hospitalizacionRepositorio.findByPacienteIdAndEstado(1L, "ACTIVA")).thenReturn(Optional.empty());
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> hospitalizacionServicio.ingresarPaciente(requestDTO));
        assertEquals("La jaula solicitada no está DISPONIBLE.", exception.getMessage());
    }

    @Test
    void darDeAlta_Exito() {
        when(hospitalizacionRepositorio.findById(10L)).thenReturn(Optional.of(hospitalizacion));
        when(hospitalizacionRepositorio.save(any(Hospitalizacion.class))).thenReturn(hospitalizacion);

        HospitalizacionResponseDTO response = hospitalizacionServicio.darDeAlta(10L);

        assertNotNull(response);
        assertEquals("DADA_DE_ALTA", hospitalizacion.getEstado());
        assertNotNull(hospitalizacion.getFechaAlta());
        assertEquals("DISPONIBLE", jaula.getEstado());
        verify(jaulaRepositorio).save(jaula);
    }

    @Test
    void trasladarPaciente_Exito() {
        Jaula nuevaJaula = new Jaula();
        nuevaJaula.setId(2L);
        nuevaJaula.setEstado("DISPONIBLE");

        when(hospitalizacionRepositorio.findById(10L)).thenReturn(Optional.of(hospitalizacion));
        when(jaulaRepositorio.findById(2L)).thenReturn(Optional.of(nuevaJaula));
        when(hospitalizacionRepositorio.save(any())).thenReturn(hospitalizacion);

        HospitalizacionResponseDTO response = hospitalizacionServicio.trasladarPaciente(10L, 2L);

        assertNotNull(response);
        assertEquals("DISPONIBLE", jaula.getEstado());
        assertEquals("OCUPADA", nuevaJaula.getEstado());
        assertEquals(nuevaJaula, hospitalizacion.getJaula());
        verify(jaulaRepositorio).save(jaula);
        verify(jaulaRepositorio).save(nuevaJaula);
    }
}
