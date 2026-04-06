package com.veterinaria.servicios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.SedeRequestDTO;
import com.veterinaria.dtos.SedeResponseDTO;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.SedeRepositorio;

@Service
public class SedeServicio {

    private final SedeRepositorio sedeRepositorio;

    public SedeServicio(SedeRepositorio sedeRepositorio) {
        this.sedeRepositorio = sedeRepositorio;
    }

    public SedeResponseDTO guardar(SedeRequestDTO dto) {
        if (sedeRepositorio.existsByNombre(dto.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una sede con este nombre");
        }
        Sede sede = new Sede();
        sede.setNombre(dto.getNombre());
        sede.setDireccion(dto.getDireccion());
        sede.setTelefono(dto.getTelefono());
        Sede guardada = sedeRepositorio.save(sede);
        return mapear(guardada);
    }

    public Page<SedeResponseDTO> listarTodas(Pageable pageable) {
        return sedeRepositorio.findAll(pageable).map(this::mapear);
    }

    public SedeResponseDTO buscarPorId(Long id) {
        Sede sede = sedeRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));
        return mapear(sede);
    }

    public SedeResponseDTO actualizar(Long id, SedeRequestDTO dto) {
        Sede sede = sedeRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));
        
        if (!sede.getNombre().equals(dto.getNombre()) && sedeRepositorio.existsByNombre(dto.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otra sede con este nombre");
        }
        
        sede.setNombre(dto.getNombre());
        sede.setDireccion(dto.getDireccion());
        sede.setTelefono(dto.getTelefono());
        Sede guardada = sedeRepositorio.save(sede);
        return mapear(guardada);
    }

    public void cambiarEstado(Long id, Boolean estado) {
        Sede sede = sedeRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));
        sede.setActivo(estado);
        sedeRepositorio.save(sede);
    }

    private SedeResponseDTO mapear(Sede sede) {
        return new SedeResponseDTO(sede.getId(), sede.getNombre(), sede.getDireccion(), sede.getTelefono(), sede.getActivo());
    }
}
