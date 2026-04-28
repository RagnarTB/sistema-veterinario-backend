package com.veterinaria.servicios;

import com.veterinaria.dtos.CitaRequestDTO;
import com.veterinaria.dtos.CitaResponseDTO;
import com.veterinaria.dtos.SlotDisponibilidadDTO;
import com.veterinaria.modelos.*;
import com.veterinaria.modelos.Enums.EstadoCita;
import com.veterinaria.respositorios.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CitaServicioTest {

    @Mock
    private CitaRepositorio citaRepositorio;
    @Mock
    private PacienteRepositorio pacienteRepositorio;
    @Mock
    private ServicioMedicoRepositorio servicioRepositorio;
    @Mock
    private EmpleadoRepositorio empleadoRepositorio;
    @Mock
    private HorarioVeterinarioRepositorio horarioRepositorio;
    @Mock
    private DiaBloqueadoRepositorio diaBloqueadoRepositorio;
    @Mock
    private SedeRepositorio sedeRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    private CitaRequestDTO requestDTO;
    private ServicioMedico servicio;
    private Empleado veterinario;
    private Sede sede;
    private Paciente paciente;
    private Cliente cliente;
    private Cita cita;

    @BeforeEach
    void setUp() {
        requestDTO = new CitaRequestDTO();
        requestDTO.setFecha(LocalDate.now().plusDays(1));
        requestDTO.setHoraInicio(LocalTime.of(10, 0));
        requestDTO.setServicioId(1L);
        requestDTO.setVeterinarioId(1L);
        requestDTO.setSedeId(1L);
        requestDTO.setPacienteIds(List.of(100L));

        servicio = new ServicioMedico();
        servicio.setId(1L);
        servicio.setDuracionMinutos(30);
        servicio.setBufferMinutos(10);
        servicio.setNombre("Consulta General");

        veterinario = new Empleado();
        veterinario.setId(1L);

        sede = new Sede();
        sede.setId(1L);

        cliente = new Cliente();
        cliente.setId(1L);

        paciente = new Paciente();
        paciente.setId(100L);
        paciente.setCliente(cliente);

        cita = new Cita();
        cita.setId(1L);
        cita.setEstado(EstadoCita.AGENDADA);
        cita.setPacientes(List.of(paciente));
        cita.setServicio(servicio);
        cita.setVeterinario(veterinario);
        cita.setSede(sede);
    }

    @Test
    void guardar_Exito() {
        when(servicioRepositorio.findById(1L)).thenReturn(Optional.of(servicio));
        when(empleadoRepositorio.findById(1L)).thenReturn(Optional.of(veterinario));
        when(sedeRepositorio.findById(1L)).thenReturn(Optional.of(sede));
        when(pacienteRepositorio.findAllById(List.of(100L))).thenReturn(List.of(paciente));
        
        when(citaRepositorio.existeCruceDeHorario(eq(1L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), eq(-1L), anyList())).thenReturn(false);
        when(citaRepositorio.save(any(Cita.class))).thenReturn(cita);

        CitaResponseDTO response = citaServicio.guardar(requestDTO);

        assertNotNull(response);
        verify(citaRepositorio).save(any(Cita.class));
    }

    @Test
    void guardar_FallaCruceHorario() {
        when(servicioRepositorio.findById(1L)).thenReturn(Optional.of(servicio));
        when(empleadoRepositorio.findById(1L)).thenReturn(Optional.of(veterinario));
        when(sedeRepositorio.findById(1L)).thenReturn(Optional.of(sede));
        when(pacienteRepositorio.findAllById(List.of(100L))).thenReturn(List.of(paciente));
        
        when(citaRepositorio.existeCruceDeHorario(eq(1L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class), eq(-1L), anyList())).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> citaServicio.guardar(requestDTO));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("El veterinario ya tiene una cita ocupando este horario."));
    }

    @Test
    void guardar_FallaFechaPasada() {
        requestDTO.setFecha(LocalDate.now().minusDays(1)); // Ayer

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> citaServicio.guardar(requestDTO));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("No se permiten citas en el pasado"));
    }

    @Test
    void obtenerDisponibilidad_ExitoConUnHueco() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        when(diaBloqueadoRepositorio.estaBloqueadoElDia(fecha, 1L)).thenReturn(false);
        
        HorarioVeterinario horario = new HorarioVeterinario();
        horario.setHoraEntrada(LocalTime.of(9, 0));
        horario.setHoraSalida(LocalTime.of(10, 0)); // Solo trabaja 1 hora
        horario.setInicioRefrigerio(null);
        horario.setFinRefrigerio(null);

        when(horarioRepositorio.findByVeterinarioIdAndDiaSemanaAndSedeId(1L, fecha.getDayOfWeek(), 1L))
                .thenReturn(Optional.of(horario));
        
        when(servicioRepositorio.findById(1L)).thenReturn(Optional.of(servicio)); // Duracion total por paciente es 40 min

        // Tiene una cita preexistente de 9:00 a 9:20
        Cita citaBloqueante = new Cita();
        citaBloqueante.setHoraInicio(LocalTime.of(9, 0));
        citaBloqueante.setHoraFin(LocalTime.of(9, 20));
        when(citaRepositorio.buscarCitasAgendadasDelDia(eq(1L), eq(fecha), anyList())).thenReturn(List.of(citaBloqueante));

        List<SlotDisponibilidadDTO> slots = citaServicio.obtenerDisponibilidad(1L, fecha, 1L, 1L, 1);

        assertNotNull(slots);
        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(9, 20), slots.get(0).getHoraInicio());
        assertEquals(LocalTime.of(10, 0), slots.get(0).getHoraFin()); // 40 minutos de duracion
    }
}
