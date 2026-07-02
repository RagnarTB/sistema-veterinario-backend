package com.veterinaria.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dtos.AuditoriaKardexDTO;
import com.veterinaria.modelos.AuditoriaKardex;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.AuditoriaKardexRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditoriaServicio {

    private final AuditoriaKardexRepositorio auditoriaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public AuditoriaServicio(AuditoriaKardexRepositorio auditoriaRepositorio,
                             PacienteRepositorio pacienteRepositorio,
                             UsuarioAutenticadoService usuarioAutenticadoService) {
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Transactional
    public void registrarAccion(Long pacienteId, String accion, String modulo, String detalle, String motivo) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        Usuario usuario = usuarioAutenticadoService.obtenerUsuarioActual();
        String nombreUsuario = usuario != null ? usuario.getNombre() + " " + usuario.getApellido() : "Sistema";

        AuditoriaKardex kardex = new AuditoriaKardex();
        kardex.setPaciente(paciente);
        kardex.setUsuario(nombreUsuario);
        kardex.setAccion(accion);
        kardex.setModulo(modulo);
        kardex.setDetalle(detalle);
        kardex.setMotivo(motivo);
        kardex.setFechaHora(LocalDateTime.now());

        auditoriaRepositorio.save(kardex);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaKardexDTO> obtenerKardexPorPaciente(Long pacienteId) {
        return auditoriaRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteId).stream()
                .map(k -> new AuditoriaKardexDTO(
                        k.getId(),
                        k.getPaciente().getId(),
                        k.getFechaHora(),
                        k.getUsuario(),
                        k.getAccion(),
                        k.getModulo(),
                        k.getDetalle(),
                        k.getMotivo()
                ))
                .collect(Collectors.toList());
    }
}
