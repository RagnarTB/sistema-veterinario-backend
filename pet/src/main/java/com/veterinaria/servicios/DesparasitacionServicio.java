package com.veterinaria.servicios;

import com.veterinaria.dtos.DesparasitacionRequestDTO;
import com.veterinaria.dtos.DesparasitacionResponseDTO;
import com.veterinaria.modelos.Desparasitacion;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.respositorios.DesparasitacionRepositorio;
import com.veterinaria.respositorios.EmpleadoRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesparasitacionServicio {

    private final DesparasitacionRepositorio desparasitacionRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final EmpleadoAutenticadoService empleadoAutenticadoService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final AuditoriaServicio auditoriaServicio;

    @Transactional
    public DesparasitacionResponseDTO guardar(DesparasitacionRequestDTO requestDTO) {
        Paciente paciente = pacienteRepositorio.findById(requestDTO.getPacienteId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con ID: " + requestDTO.getPacienteId()));

        Empleado empleado = empleadoAutenticadoService.obtenerEmpleadoActual();

        Desparasitacion desparasitacion = new Desparasitacion();
        desparasitacion.setTipo(requestDTO.getTipo());
        desparasitacion.setProductoUtilizado(requestDTO.getProductoUtilizado());
        desparasitacion.setPesoAlMomento(requestDTO.getPesoAlMomento());
        desparasitacion.setFechaAplicacion(requestDTO.getFechaAplicacion());
        desparasitacion.setFechaProximaDosis(requestDTO.getFechaProximaDosis());
        desparasitacion.setObservaciones(requestDTO.getObservaciones());
        desparasitacion.setPaciente(paciente);
        desparasitacion.setEmpleado(empleado);

        Desparasitacion desparasitacionGuardada = desparasitacionRepositorio.save(desparasitacion);
        return mapearADTO(desparasitacionGuardada);
    }

    @Transactional(readOnly = true)
    public List<DesparasitacionResponseDTO> listarPorPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con ID: " + pacienteId));
        
        if (usuarioAutenticadoService.esCliente()) {
            Long clienteActualId = usuarioAutenticadoService.obtenerClienteActual().getId();
            if (paciente.getCliente() == null || !paciente.getCliente().getId().equals(clienteActualId)) {
                throw new com.veterinaria.excepciones.BusinessLogicException("No tienes permiso para ver el historial de esta mascota.");
            }
        }

        List<Desparasitacion> desparasitaciones = desparasitacionRepositorio.findByPacienteIdOrderByFechaAplicacionDesc(pacienteId);
        return desparasitaciones.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    private DesparasitacionResponseDTO mapearADTO(Desparasitacion desparasitacion) {
        DesparasitacionResponseDTO dto = new DesparasitacionResponseDTO();
        dto.setId(desparasitacion.getId());
        dto.setTipo(desparasitacion.getTipo());
        dto.setProductoUtilizado(desparasitacion.getProductoUtilizado());
        dto.setPesoAlMomento(desparasitacion.getPesoAlMomento());
        dto.setFechaAplicacion(desparasitacion.getFechaAplicacion());
        dto.setFechaProximaDosis(desparasitacion.getFechaProximaDosis());
        dto.setObservaciones(desparasitacion.getObservaciones());
        dto.setPacienteId(desparasitacion.getPaciente().getId()); // Note: Paciente entity has private Long Id; getter might be getId() or getId(), assuming getId() because of lombok @Data.
        dto.setPacienteNombre(desparasitacion.getPaciente().getNombre());
        dto.setEmpleadoId(desparasitacion.getEmpleado().getId());
        dto.setEmpleadoNombre(desparasitacion.getEmpleado().getUsuario().getNombre() + " " + desparasitacion.getEmpleado().getUsuario().getApellido());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<DesparasitacionResponseDTO> listarProximasDosis() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate enDosSemanas = hoy.plusDays(14);
        List<Desparasitacion> proximas = desparasitacionRepositorio.findByFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(hoy, enDosSemanas);
        return proximas.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    @Transactional
    public DesparasitacionResponseDTO actualizarProximaDosis(Long id, java.time.LocalDate nuevaFecha) {
        Desparasitacion desparasitacion = desparasitacionRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Desparasitación no encontrada"));
        
        String detalle = "Cambio de próxima dosis de " + 
                        (desparasitacion.getFechaProximaDosis() != null ? desparasitacion.getFechaProximaDosis().toString() : "N/A") + 
                        " a " + (nuevaFecha != null ? nuevaFecha.toString() : "N/A");
        
        desparasitacion.setFechaProximaDosis(nuevaFecha);
        Desparasitacion actualizada = desparasitacionRepositorio.save(desparasitacion);
        
        auditoriaServicio.registrarAccion(desparasitacion.getPaciente().getId(), "EDICION", "DESPARASITACION", detalle, null);
        
        return mapearADTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id, String motivo) {
        if (!usuarioAutenticadoService.tieneRol("ROLE_ADMIN")) {
            throw new com.veterinaria.excepciones.BusinessLogicException("Solo los administradores pueden eliminar registros del historial.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new com.veterinaria.excepciones.BusinessLogicException("El motivo de eliminación es obligatorio.");
        }

        Desparasitacion desparasitacion = desparasitacionRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Desparasitación no encontrada"));
        
        String detalle = "Eliminación de desparasitación (" + desparasitacion.getTipo() + ") con producto: " + desparasitacion.getProductoUtilizado();
        
        auditoriaServicio.registrarAccion(desparasitacion.getPaciente().getId(), "ELIMINACION", "DESPARASITACION", detalle, motivo);
        
        desparasitacionRepositorio.delete(desparasitacion);
    }
}
