package com.veterinaria.servicios;

import com.veterinaria.dtos.JaulaRequestDTO;
import com.veterinaria.dtos.JaulaResponseDTO;
import com.veterinaria.modelos.Jaula;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.JaulaRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JaulaServicio {

    private final JaulaRepositorio jaulaRepositorio;
    private final SedeRepositorio sedeRepositorio;

    @Transactional
    public JaulaResponseDTO guardar(JaulaRequestDTO requestDTO) {
        Sede sede = sedeRepositorio.findById(requestDTO.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con ID: " + requestDTO.getSedeId()));

        Jaula jaula = new Jaula();
        jaula.setNumero(requestDTO.getNumero());
        jaula.setTipo(requestDTO.getTipo());
        jaula.setEstado(requestDTO.getEstado());
        jaula.setSede(sede);

        Jaula jaulaGuardada = jaulaRepositorio.save(jaula);
        return mapearADTO(jaulaGuardada);
    }

    @Transactional
    public JaulaResponseDTO actualizar(Long id, JaulaRequestDTO requestDTO) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));

        Sede sede = sedeRepositorio.findById(requestDTO.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con ID: " + requestDTO.getSedeId()));

        jaula.setNumero(requestDTO.getNumero());
        jaula.setTipo(requestDTO.getTipo());
        jaula.setEstado(requestDTO.getEstado());
        jaula.setSede(sede);

        Jaula jaulaActualizada = jaulaRepositorio.save(jaula);
        return mapearADTO(jaulaActualizada);
    }

    @Transactional(readOnly = true)
    public List<JaulaResponseDTO> listarTodas() {
        return jaulaRepositorio.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JaulaResponseDTO obtenerPorId(Long id) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));
        return mapearADTO(jaula);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!jaulaRepositorio.existsById(id)) {
            throw new EntityNotFoundException("Jaula no encontrada con ID: " + id);
        }
        jaulaRepositorio.deleteById(id);
    }

    private JaulaResponseDTO mapearADTO(Jaula jaula) {
        JaulaResponseDTO dto = new JaulaResponseDTO();
        dto.setId(jaula.getId());
        dto.setNumero(jaula.getNumero());
        dto.setTipo(jaula.getTipo());
        dto.setEstado(jaula.getEstado());
        dto.setSedeId(jaula.getSede().getId());
        dto.setSedeNombre(jaula.getSede().getNombre());
        return dto;
    }
}
