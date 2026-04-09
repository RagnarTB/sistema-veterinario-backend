package com.veterinaria.servicios;

import com.veterinaria.dtos.JaulaRequestDTO;
import com.veterinaria.dtos.JaulaResponseDTO;
import com.veterinaria.modelos.Jaula;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import com.veterinaria.respositorios.JaulaRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JaulaServicioTest {

    @Mock
    private JaulaRepositorio jaulaRepositorio;
    
    @Mock
    private SedeRepositorio sedeRepositorio;

    @Mock
    private HospitalizacionRepositorio hospitalizacionRepositorio;

    @InjectMocks
    private JaulaServicio jaulaServicio;

    private Sede sede;
    private Jaula jaula;
    private JaulaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        sede = new Sede();
        sede.setId(1L);
        sede.setNombre("Sede Central");

        jaula = new Jaula();
        jaula.setId(1L);
        jaula.setNumero("J-01");
        jaula.setTipo("Perro Grande");
        jaula.setEstado("DISPONIBLE");
        jaula.setSede(sede);
        jaula.setActivo(true);

        requestDTO = new JaulaRequestDTO();
        requestDTO.setNumero("J-01");
        requestDTO.setTipo("Perro Grande");
        requestDTO.setEstado("DISPONIBLE");
        requestDTO.setSedeId(1L);
    }

    @Test
    void guardar_Exito() {
        when(sedeRepositorio.findById(1L)).thenReturn(Optional.of(sede));
        when(jaulaRepositorio.save(any(Jaula.class))).thenReturn(jaula);

        JaulaResponseDTO response = jaulaServicio.guardar(requestDTO);

        assertNotNull(response);
        assertEquals("J-01", response.getNumero());
        verify(sedeRepositorio, times(1)).findById(1L);
        verify(jaulaRepositorio, times(1)).save(any(Jaula.class));
    }

    @Test
    void guardar_SedeNoEncontrada() {
        when(sedeRepositorio.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> jaulaServicio.guardar(requestDTO));
        verify(jaulaRepositorio, never()).save(any(Jaula.class));
    }

    @Test
    void actualizar_Exito() {
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));
        when(sedeRepositorio.findById(1L)).thenReturn(Optional.of(sede));
        when(jaulaRepositorio.save(any(Jaula.class))).thenReturn(jaula);

        JaulaResponseDTO response = jaulaServicio.actualizar(1L, requestDTO);

        assertNotNull(response);
        assertEquals("J-01", response.getNumero());
        verify(jaulaRepositorio).save(any(Jaula.class));
    }

    @Test
    void actualizar_NoPermitirLiberarJaulaConHospitalizacionActiva() {
        jaula.setEstado("OCUPADA");
        requestDTO.setEstado("DISPONIBLE");
        
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));
        when(hospitalizacionRepositorio.existsByJaulaIdAndEstado(1L, "ACTIVA")).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jaulaServicio.actualizar(1L, requestDTO));
        assertEquals("No se puede liberar la jaula, tiene una hospitalización activa. Debe dar de alta al paciente.", exception.getMessage());
    }

    @Test
    void listarTodas_Exito() {
        when(jaulaRepositorio.findByActivoTrue()).thenReturn(List.of(jaula));

        List<JaulaResponseDTO> result = jaulaServicio.listarTodas();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("J-01", result.get(0).getNumero());
    }

    @Test
    void obtenerPorId_Exito() {
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));

        JaulaResponseDTO result = jaulaServicio.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals("J-01", result.getNumero());
    }

    @Test
    void eliminar_Exito() {
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));
        when(hospitalizacionRepositorio.existsByJaulaIdAndEstado(1L, "ACTIVA")).thenReturn(false);

        jaulaServicio.eliminar(1L);

        assertFalse(jaula.getActivo());
        verify(jaulaRepositorio, times(1)).save(jaula);
    }
    
    @Test
    void eliminar_LanzaExcepcionSiHospitalizacionActiva() {
        when(jaulaRepositorio.findById(1L)).thenReturn(Optional.of(jaula));
        when(hospitalizacionRepositorio.existsByJaulaIdAndEstado(1L, "ACTIVA")).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jaulaServicio.eliminar(1L));
        assertEquals("No se puede eliminar la jaula, tiene una hospitalización activa.", exception.getMessage());
        verify(jaulaRepositorio, never()).save(jaula);
    }
}
