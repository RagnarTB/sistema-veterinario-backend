package com.veterinaria.servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.CitaRequestDTO;
import com.veterinaria.dtos.CitaResponseDTO;
import com.veterinaria.modelos.Cita;
import com.veterinaria.modelos.EstadoCita;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.respositorios.CitaRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;

@Service
public class CitaServicio {

    private CitaRepositorio citaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;

    public CitaServicio(CitaRepositorio citaRepositorio, PacienteRepositorio pacienteRepositorio) {
        this.citaRepositorio = citaRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
    }

    public CitaResponseDTO guardar(CitaRequestDTO dto) {
        Paciente paciente = pacienteRepositorio.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se puede crear la cita, paciente no encontrado : " + dto.getPacienteId())

                );

        Cita cita = new Cita();
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setFecha(dto.getFecha());
        cita.setHora(dto.getHora());
        cita.setMotivo(dto.getMotivo());
        cita.setPaciente(paciente);

        Cita citaGuardada = citaRepositorio.save(cita);

        CitaResponseDTO respuesta = new CitaResponseDTO();
        respuesta.setId(citaGuardada.getId());
        respuesta.setEstado(citaGuardada.getEstado());
        respuesta.setFecha(citaGuardada.getFecha());
        respuesta.setHora(citaGuardada.getHora());
        respuesta.setMotivo(citaGuardada.getMotivo());
        respuesta.setPacienteId(paciente.getId());
        return respuesta;

    }

    public List<CitaResponseDTO> listar() {
        List<Cita> citas = citaRepositorio.findAll();
        return citas.stream().map(cita -> new CitaResponseDTO(
                cita.getId(),
                cita.getFecha(),
                cita.getHora(),
                cita.getMotivo(),
                cita.getEstado(),
                cita.getPaciente().getId()))
                .collect(Collectors.toList());
    }

    public CitaResponseDTO buscarPorId(Long id) {
        return citaRepositorio.findById(id)
                .map(cita -> new CitaResponseDTO(
                        cita.getId(),
                        cita.getFecha(),
                        cita.getHora(),
                        cita.getMotivo(),
                        cita.getEstado(),
                        cita.getPaciente().getId()))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada con ID: " + id));
    }

    public CitaResponseDTO actualizar(Long id, CitaRequestDTO dto) {
        Cita citaDb = citaRepositorio.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada con ID: " + id));

        Paciente paciente = pacienteRepositorio.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se puede actualizar la cita, paciente no encontrado : " + dto.getPacienteId())

                );

        citaDb.setFecha(dto.getFecha());
        citaDb.setHora(dto.getHora());
        citaDb.setMotivo(dto.getMotivo());
        citaDb.setPaciente(paciente);

        Cita citaGuardada = citaRepositorio.save(citaDb);

        return new CitaResponseDTO(
                citaGuardada.getId(),
                citaGuardada.getFecha(),
                citaGuardada.getHora(),
                citaGuardada.getMotivo(),
                citaGuardada.getEstado(),
                citaGuardada.getPaciente().getId());
    }

    public void eliminar(Long id) {
        Cita citaDb = citaRepositorio.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada con ID: " + id));
        citaRepositorio.delete(citaDb);
    }

}
