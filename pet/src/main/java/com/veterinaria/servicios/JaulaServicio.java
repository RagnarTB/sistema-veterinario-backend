package com.veterinaria.servicios;

import com.veterinaria.dtos.JaulaRequestDTO;
import com.veterinaria.dtos.JaulaResponseDTO;
import com.veterinaria.modelos.Jaula;
import com.veterinaria.modelos.Sede;
import com.veterinaria.modelos.CategoriaJaula;
import com.veterinaria.modelos.TamanoJaula;
import com.veterinaria.respositorios.JaulaRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import com.veterinaria.respositorios.CategoriaJaulaRepositorio;
import com.veterinaria.respositorios.TamanoJaulaRepositorio;
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
    private final HospitalizacionRepositorio hospitalizacionRepositorio;
    private final CategoriaJaulaRepositorio categoriaJaulaRepositorio;
    private final TamanoJaulaRepositorio tamanoJaulaRepositorio;

    @Transactional
    public JaulaResponseDTO guardar(JaulaRequestDTO requestDTO) {
        Sede sede = sedeRepositorio.findById(requestDTO.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con ID: " + requestDTO.getSedeId()));

        Jaula jaula = new Jaula();
        jaula.setNumero(requestDTO.getNumero());
        jaula.setEstado(requestDTO.getEstado());
        jaula.setSede(sede);
        jaula.setAlertaContagio(requestDTO.getAlertaContagio() != null ? requestDTO.getAlertaContagio() : false);
        
        if (requestDTO.getCategoriaId() != null) {
            CategoriaJaula cat = categoriaJaulaRepositorio.findById(requestDTO.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categora no encontrada"));
            jaula.setCategoria(cat);
        }
        
        if (requestDTO.getTamanoId() != null) {
            TamanoJaula tam = tamanoJaulaRepositorio.findById(requestDTO.getTamanoId())
                    .orElseThrow(() -> new EntityNotFoundException("Tamao no encontrado"));
            jaula.setTamano(tam);
        }

        Jaula jaulaGuardada = jaulaRepositorio.save(jaula);
        return mapearADTO(jaulaGuardada);
    }

    @Transactional
    public JaulaResponseDTO actualizar(Long id, JaulaRequestDTO requestDTO) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));

        if (jaula.getEstado().equals("OCUPADA") && requestDTO.getEstado().equals("DISPONIBLE")) {
            if (hospitalizacionRepositorio.existsByJaulaIdAndEstado(id, "ACTIVA")) {
                throw new IllegalStateException("No se puede liberar la jaula, tiene una hospitalizacin activa. Debe dar de alta al paciente.");
            }
        }

        Sede sede = sedeRepositorio.findById(requestDTO.getSedeId())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con ID: " + requestDTO.getSedeId()));

        jaula.setNumero(requestDTO.getNumero());
        jaula.setEstado(requestDTO.getEstado());
        jaula.setSede(sede);
        jaula.setAlertaContagio(requestDTO.getAlertaContagio() != null ? requestDTO.getAlertaContagio() : false);
        
        if (requestDTO.getCategoriaId() != null) {
            CategoriaJaula cat = categoriaJaulaRepositorio.findById(requestDTO.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categora no encontrada"));
            jaula.setCategoria(cat);
        }
        
        if (requestDTO.getTamanoId() != null) {
            TamanoJaula tam = tamanoJaulaRepositorio.findById(requestDTO.getTamanoId())
                    .orElseThrow(() -> new EntityNotFoundException("Tamao no encontrado"));
            jaula.setTamano(tam);
        }

        Jaula jaulaActualizada = jaulaRepositorio.save(jaula);
        return mapearADTO(jaulaActualizada);
    }

    @Transactional(readOnly = true)
    public List<JaulaResponseDTO> listarTodas() {
        return jaulaRepositorio.findByActivoTrue().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JaulaResponseDTO obtenerPorId(Long id) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));
        return mapearADTO(jaula);
    }

    @Transactional(readOnly = true)
    public List<JaulaResponseDTO> listarPorSede(Long sedeId) {
        return jaulaRepositorio.findBySedeIdAndActivoTrue(sedeId).stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public JaulaResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));

        if ("DISPONIBLE".equals(nuevoEstado) && hospitalizacionRepositorio.existsByJaulaIdAndEstado(id, "ACTIVA")) {
            throw new IllegalStateException("No se puede liberar la jaula, tiene una hospitalizacion activa.");
        }

        jaula.setEstado(nuevoEstado);
        Jaula guardada = jaulaRepositorio.save(jaula);
        return mapearADTO(guardada);
    }

    @Transactional
    public void eliminar(Long id) {
        Jaula jaula = jaulaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + id));
        
        if (hospitalizacionRepositorio.existsByJaulaIdAndEstado(id, "ACTIVA")) {
            throw new IllegalStateException("No se puede eliminar la jaula, tiene una hospitalizacin activa.");
        }
        
        jaula.setActivo(false);
        jaulaRepositorio.save(jaula);
    }

    private JaulaResponseDTO mapearADTO(Jaula jaula) {
        JaulaResponseDTO dto = new JaulaResponseDTO();
        dto.setId(jaula.getId());
        dto.setNumero(jaula.getNumero());
        dto.setEstado(jaula.getEstado());
        dto.setSedeId(jaula.getSede().getId());
        dto.setSedeNombre(jaula.getSede().getNombre());
        dto.setActivo(jaula.getActivo());
        dto.setAlertaContagio(jaula.getAlertaContagio());
        if (jaula.getCategoria() != null) {
            dto.setCategoriaNombre(jaula.getCategoria().getNombre());
        }
        if (jaula.getTamano() != null) {
            dto.setTamanoNombre(jaula.getTamano().getNombre());
            dto.setTamanoId(jaula.getTamano().getId());
        }
        return dto;
    }
}
