package com.veterinaria.servicios;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veterinaria.excepciones.ResourceNotFoundException;
import com.veterinaria.excepciones.BusinessLogicException;

import com.veterinaria.dtos.CitaRequestDTO;
import com.veterinaria.dtos.CitaResponseDTO;
import com.veterinaria.dtos.PacienteResumenDTO;
import com.veterinaria.dtos.SlotDisponibilidadDTO;
import com.veterinaria.modelos.Cita;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.HorarioVeterinario;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.modelos.ServicioMedico;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Enums.EstadoCita;
import com.veterinaria.respositorios.CitaRepositorio;
import com.veterinaria.respositorios.DiaBloqueadoRepositorio;
import com.veterinaria.respositorios.HorarioVeterinarioRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;
import com.veterinaria.respositorios.ServicioMedicoRepositorio;
import com.veterinaria.respositorios.EmpleadoRepositorio;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.SedeRepositorio;

@Service
public class CitaServicio {

        private final CitaRepositorio citaRepositorio;
        private final PacienteRepositorio pacienteRepositorio;
        private final ServicioMedicoRepositorio servicioRepositorio;
        private final EmpleadoRepositorio empleadoRepositorio;
        private final List<EstadoCita> ESTADOS_IGNORADOS = List.of(EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

        private final HorarioVeterinarioRepositorio horarioRepositorio;
        private final DiaBloqueadoRepositorio diaBloqueadoRepositorio;
        private final SedeRepositorio sedeRepositorio;

        public CitaServicio(CitaRepositorio citaRepositorio, PacienteRepositorio pacienteRepositorio,
                        ServicioMedicoRepositorio servicioRepositorio, EmpleadoRepositorio empleadoRepositorio,
                        HorarioVeterinarioRepositorio horarioRepositorio,
                        DiaBloqueadoRepositorio diaBloqueadoRepositorio,
                        SedeRepositorio sedeRepositorio) {
                this.citaRepositorio = citaRepositorio;
                this.pacienteRepositorio = pacienteRepositorio;
                this.servicioRepositorio = servicioRepositorio;
                this.empleadoRepositorio = empleadoRepositorio;
                this.horarioRepositorio = horarioRepositorio;
                this.diaBloqueadoRepositorio = diaBloqueadoRepositorio;
                this.sedeRepositorio = sedeRepositorio;
        }

        @Transactional
        public CitaResponseDTO guardar(CitaRequestDTO dto) {
                validarFechaHoraNoPasado(dto.getFecha(), dto.getHoraInicio());
                ServicioMedico servicio = servicioRepositorio.findById(dto.getServicioId())
                                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

                if (servicio.getActivo() != null && !servicio.getActivo()) {
                        throw new BusinessLogicException("El servicio médico seleccionado está desactivado.");
                }

                Empleado veterinario = empleadoRepositorio.findById(dto.getVeterinarioId())
                                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));

                if (!veterinario.getActivo()) {
                        throw new BusinessLogicException("El veterinario seleccionado está desactivado.");
                }

                Sede sede = sedeRepositorio.findById(dto.getSedeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

                List<Paciente> pacientes = pacienteRepositorio.findAllById(dto.getPacienteIds());
                if (pacientes.isEmpty()) {
                        throw new ResourceNotFoundException("No se encontraron los pacientes");
                }

                for (Paciente p : pacientes) {
                        if (!p.getActivo()) {
                                throw new BusinessLogicException("El paciente \"" + p.getNombre() + "\" está desactivado y no puede ser agendado.");
                        }
                }

                Cliente clienteBase = pacientes.get(0).getCliente();
                if (pacientes.stream().anyMatch(p -> !p.getCliente().equals(clienteBase))) {
                        throw new BusinessLogicException("Todos los pacientes de la cita deben pertenecer al mismo cliente. No se permiten mascotas de distintos dueños en una misma cita.");
                }

                int cantidadMascotas = dto.getPacienteIds().size();
                int tiempoTotalOcupado = (servicio.getDuracionMinutos() + servicio.getBufferMinutos())
                                * cantidadMascotas;
                LocalTime horaFinCalculada = dto.getHoraInicio().plusMinutes(tiempoTotalOcupado);

                citaRepositorio.buscarCitasAgendadasDelDiaConLock(veterinario.getId(), dto.getFecha(), ESTADOS_IGNORADOS);

                boolean existeCruce = citaRepositorio.existeCruceDeHorario(
                                veterinario.getId(), dto.getFecha(), dto.getHoraInicio(),
                                horaFinCalculada, -1L, ESTADOS_IGNORADOS);

                if (existeCruce) {
                        throw new BusinessLogicException("El veterinario ya tiene una cita ocupando este horario.");
                }

                Cita cita = new Cita();
                cita.setEstado(EstadoCita.AGENDADA);
                cita.setFecha(dto.getFecha());
                cita.setHoraInicio(dto.getHoraInicio());
                cita.setHoraFin(horaFinCalculada);
                cita.setMotivo(dto.getMotivo());
                cita.setServicio(servicio);
                cita.setVeterinario(veterinario);
                cita.setSede(sede);
                cita.setPacientes(pacientes);

                Cita citaGuardada = citaRepositorio.save(cita);
                return mapearAResponse(citaGuardada);
        }

        public Page<CitaResponseDTO> listar(Long sedeId, String buscar, Pageable pageable) {
                if (pageable.getSort().isUnsorted()) {
                        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                        Sort.by("fecha").ascending().and(Sort.by("horaInicio").ascending()));
                }
                Page<Cita> pagina;
                if (buscar != null && !buscar.trim().isEmpty()) {
                        pagina = citaRepositorio.buscarEnSede(sedeId, buscar, pageable);
                } else {
                        pagina = citaRepositorio.findBySedeId(sedeId, pageable);
                }
                return pagina.map(this::mapearAResponse);
        }

        public Page<CitaResponseDTO> listarConFiltros(Long sedeId, LocalDate fecha, EstadoCita estado,
                        Long veterinarioId, String buscar, Pageable pageable) {
                if (pageable.getSort().isUnsorted()) {
                        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                        Sort.by("fecha").ascending().and(Sort.by("horaInicio").ascending()));
                }
                Page<Cita> pagina;

                if (fecha != null && veterinarioId != null && estado != null) {
                        pagina = citaRepositorio.findBySedeIdAndFechaAndVeterinarioIdAndEstado(sedeId, fecha, veterinarioId, estado, pageable);
                } else if (fecha != null && veterinarioId != null) {
                        pagina = citaRepositorio.findBySedeIdAndFechaAndVeterinarioId(sedeId, fecha, veterinarioId, pageable);
                } else if (fecha != null && estado != null) {
                        pagina = citaRepositorio.findBySedeIdAndFechaAndEstado(sedeId, fecha, estado, pageable);
                } else if (fecha != null) {
                        pagina = citaRepositorio.findBySedeIdAndFecha(sedeId, fecha, pageable);
                } else if (buscar != null && !buscar.trim().isEmpty()) {
                        pagina = citaRepositorio.buscarEnSede(sedeId, buscar, pageable);
                } else {
                        pagina = citaRepositorio.findBySedeId(sedeId, pageable);
                }
                return pagina.map(this::mapearAResponse);
        }

        public CitaResponseDTO buscarPorId(Long id) {
                return citaRepositorio.findById(id)
                                .map(this::mapearAResponse)
                                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        }

        @Transactional
        public CitaResponseDTO actualizar(Long id, CitaRequestDTO dto) {
                validarFechaHoraNoPasado(dto.getFecha(), dto.getHoraInicio());
                Cita citaDb = citaRepositorio.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

                ServicioMedico servicio = servicioRepositorio.findById(dto.getServicioId())
                                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

                if (servicio.getActivo() != null && !servicio.getActivo()) {
                        throw new BusinessLogicException("El servicio médico seleccionado está desactivado.");
                }

                Empleado veterinario = empleadoRepositorio.findById(dto.getVeterinarioId())
                                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado"));

                if (!veterinario.getActivo()) {
                        throw new BusinessLogicException("El veterinario seleccionado está desactivado.");
                }

                Sede sede = sedeRepositorio.findById(dto.getSedeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

                List<Paciente> pacientes = pacienteRepositorio.findAllById(dto.getPacienteIds());
                if (pacientes.isEmpty()) {
                        throw new ResourceNotFoundException("No se encontraron los pacientes");
                }

                for (Paciente p : pacientes) {
                        if (!p.getActivo()) {
                                throw new BusinessLogicException("El paciente \"" + p.getNombre() + "\" está desactivado y no puede ser agendado.");
                        }
                }

                Cliente clienteBase = pacientes.get(0).getCliente();
                if (pacientes.stream().anyMatch(p -> !p.getCliente().equals(clienteBase))) {
                        throw new BusinessLogicException("Todos los pacientes de la cita deben pertenecer al mismo cliente.");
                }

                int cantidadMascotas = dto.getPacienteIds().size();
                int tiempoTotalOcupado = (servicio.getDuracionMinutos() + servicio.getBufferMinutos()) * cantidadMascotas;
                LocalTime horaFinCalculada = dto.getHoraInicio().plusMinutes(tiempoTotalOcupado);

                citaRepositorio.buscarCitasAgendadasDelDiaConLock(veterinario.getId(), dto.getFecha(), ESTADOS_IGNORADOS);

                boolean existeCruce = citaRepositorio.existeCruceDeHorario(
                                veterinario.getId(), dto.getFecha(), dto.getHoraInicio(),
                                horaFinCalculada, id, ESTADOS_IGNORADOS);

                if (existeCruce) {
                        throw new BusinessLogicException("No se puede reprogramar: El veterinario ya tiene otro compromiso en ese horario.");
                }

                citaDb.setFecha(dto.getFecha());
                citaDb.setHoraInicio(dto.getHoraInicio());
                citaDb.setHoraFin(horaFinCalculada);
                citaDb.setMotivo(dto.getMotivo());
                citaDb.setServicio(servicio);
                citaDb.setVeterinario(veterinario);
                citaDb.setSede(sede);
                citaDb.setPacientes(pacientes);

                Cita citaGuardada = citaRepositorio.save(citaDb);
                return mapearAResponse(citaGuardada);
        }

        @Transactional
        public void eliminar(Long id) {
                Cita citaDb = citaRepositorio.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

                if (citaDb.getEstado() != EstadoCita.AGENDADA) {
                        throw new BusinessLogicException("No se puede eliminar una cita que ya fue " + citaDb.getEstado()
                                                        + ". Estado actual: " + citaDb.getEstado());
                }

                citaDb.setEstado(EstadoCita.CANCELADA);
                citaRepositorio.save(citaDb);
        }

        private CitaResponseDTO mapearAResponse(Cita cita) {
                List<Long> pacientesIds = cita.getPacientes().stream()
                                .map(Paciente::getId)
                                .collect(Collectors.toList());

                List<PacienteResumenDTO> pacientesResumen = cita.getPacientes().stream()
                                .map(p -> {
                                        String clienteNombre = null;
                                        Long clienteId = null;
                                        if (p.getCliente() != null) {
                                                clienteId = p.getCliente().getId();
                                                if (p.getCliente().getUsuario() != null) {
                                                        clienteNombre = p.getCliente().getUsuario().getNombre()
                                                                + " " + p.getCliente().getUsuario().getApellido();
                                                }
                                        }
                                        return new PacienteResumenDTO(
                                                p.getId(),
                                                p.getNombre(),
                                                p.getEspecie() != null ? p.getEspecie().getNombre() : null,
                                                p.getSexo(),
                                                clienteId,
                                                clienteNombre);
                                })
                                .collect(Collectors.toList());

                String veterinarioNombre = "";
                if (cita.getVeterinario() != null && cita.getVeterinario().getUsuario() != null) {
                        veterinarioNombre = cita.getVeterinario().getUsuario().getNombre()
                                        + " " + cita.getVeterinario().getUsuario().getApellido();
                }

                return new CitaResponseDTO(
                                cita.getId(),
                                cita.getFecha(),
                                cita.getHoraInicio(),
                                cita.getHoraFin(),
                                cita.getServicio().getNombre(),
                                cita.getServicio().getId(),
                                cita.getVeterinario().getId(),
                                veterinarioNombre,
                                cita.getMotivo(),
                                cita.getEstado(),
                                pacientesIds,
                                cita.getSede().getId(),
                                cita.getSede().getNombre(),
                                pacientesResumen);
        }

        public List<SlotDisponibilidadDTO> obtenerDisponibilidad(Long veterinarioId, LocalDate fecha, Long servicioId,
                        Long sedeId, int cantidadPacientes) {
                return obtenerDisponibilidad(veterinarioId, fecha, servicioId, sedeId, cantidadPacientes, null);
        }

        public List<SlotDisponibilidadDTO> obtenerDisponibilidad(Long veterinarioId, LocalDate fecha, Long servicioId,
                        Long sedeId, int cantidadPacientes, Long citaIdExcluir) {

                if (cantidadPacientes < 1) {
                        throw new BusinessLogicException("cantidadPacientes debe ser >= 1");
                }

                if (diaBloqueadoRepositorio.estaBloqueadoElDia(fecha, veterinarioId)) {
                        return List.of();
                }

                HorarioVeterinario horario = horarioRepositorio
                                .findByVeterinarioIdAndDiaSemanaAndSedeId(veterinarioId, fecha.getDayOfWeek(), sedeId)
                                .orElse(null);

                if (horario == null) {
                        return List.of();
                }

                ServicioMedico servicio = servicioRepositorio.findById(servicioId)
                                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
                int duracionPorPaciente = (servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : 0) 
                                        + (servicio.getBufferMinutos() != null ? servicio.getBufferMinutos() : 0);
                
                // Seguridad: Si el servicio tiene duración 0, forzar al menos 15 minutos para evitar bucle infinito
                if (duracionPorPaciente <= 0) {
                        duracionPorPaciente = 15;
                }
                
                int duracionTotal = duracionPorPaciente * cantidadPacientes;

                List<Cita> citasDelDia = citaRepositorio.buscarCitasAgendadasDelDia(veterinarioId, fecha, ESTADOS_IGNORADOS);

                if (citaIdExcluir != null) {
                        citasDelDia = citasDelDia.stream()
                                .filter(c -> !c.getId().equals(citaIdExcluir))
                                .collect(Collectors.toList());
                }

                List<SlotDisponibilidadDTO> slotsDisponibles = new java.util.ArrayList<>();
                LocalTime horaActual = horario.getHoraEntrada();
                
                // Si es hoy, empezar desde la hora actual o la hora de entrada, lo que sea posterior
                if (fecha.equals(LocalDate.now())) {
                        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
                        if (ahora.isAfter(horaActual)) {
                                horaActual = ahora;
                        }
                }

                int iteraciones = 0;
                while (horaActual.plusMinutes(duracionTotal).compareTo(horario.getHoraSalida()) <= 0 && iteraciones < 500) {
                        iteraciones++;
                        LocalTime finSlot = horaActual.plusMinutes(duracionTotal);

                        boolean chocaConRefrigerio = false;
                        if (horario.getInicioRefrigerio() != null && horario.getFinRefrigerio() != null) {
                                if (horaActual.isBefore(horario.getFinRefrigerio())
                                                && finSlot.isAfter(horario.getInicioRefrigerio())) {
                                        chocaConRefrigerio = true;
                                        horaActual = horario.getFinRefrigerio();
                                        continue;
                                }
                        }

                        boolean chocaConCita = false;
                        for (Cita cita : citasDelDia) {
                                if (horaActual.isBefore(cita.getHoraFin()) && finSlot.isAfter(cita.getHoraInicio())) {
                                        chocaConCita = true;
                                        // Avanzar a la hora fin de la cita para no quedar atrapado
                                        horaActual = cita.getHoraFin();
                                        break;
                                }
                        }

                        if (!chocaConRefrigerio && !chocaConCita) {
                                slotsDisponibles.add(new SlotDisponibilidadDTO(horaActual, finSlot));
                                // Avanzar al menos duracionPorPaciente o 15 minutos para el siguiente slot
                                horaActual = horaActual.plusMinutes(Math.max(15, duracionPorPaciente));
                        }
                }

                return slotsDisponibles;
        }

        @Transactional
        public void cambiarEstado(Long id, EstadoCita nuevoEstado) {
                Cita citaDb = citaRepositorio.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

                validarTransicionEstado(citaDb.getEstado(), nuevoEstado);
                citaDb.setEstado(nuevoEstado);
                citaRepositorio.save(citaDb);
        }

        private static void validarFechaHoraNoPasado(LocalDate fecha, LocalTime horaInicio) {
                if (fecha == null || horaInicio == null) return;
                if (fecha.isBefore(LocalDate.now())) {
                        throw new BusinessLogicException("No se permiten citas en el pasado");
                }
                if (fecha.equals(LocalDate.now()) && horaInicio.isBefore(LocalTime.now().withSecond(0).withNano(0))) {
                        throw new BusinessLogicException("No se permiten citas en el pasado");
                }
        }

        private static void validarTransicionEstado(EstadoCita actual, EstadoCita nuevo) {
                if (actual == null || nuevo == null) {
                        throw new BusinessLogicException("Estado inválido");
                }
                if (!actual.puedeTransitarA(nuevo)) {
                        throw new BusinessLogicException("Transición de estado inválida: " + actual + " -> " + nuevo);
                }
        }
}