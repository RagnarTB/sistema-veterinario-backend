package com.veterinaria.servicios;

import com.veterinaria.dtos.ExamenMedicoRequestDTO;
import com.veterinaria.dtos.ExamenMedicoResponseDTO;
import com.veterinaria.modelos.AtencionMedica;
import com.veterinaria.modelos.ExamenMedico;
import com.veterinaria.respositorios.AtencionMedicaRepositorio;
import com.veterinaria.respositorios.ExamenMedicoRepositorio;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamenMedicoServicio {

    private final ExamenMedicoRepositorio examenMedicoRepositorio;
    private final AtencionMedicaRepositorio atencionMedicaRepositorio;

    @Transactional
    public ExamenMedicoResponseDTO solicitarExamen(ExamenMedicoRequestDTO requestDTO) {
        AtencionMedica atencionMedica = atencionMedicaRepositorio.findById(requestDTO.getAtencionMedicaId())
                .orElseThrow(() -> new EntityNotFoundException("Atención Médica no encontrada con ID: " + requestDTO.getAtencionMedicaId()));

        ExamenMedico examen = new ExamenMedico();
        examen.setTipoExamen(requestDTO.getTipoExamen());
        examen.setResultados(requestDTO.getResultados());
        examen.setFechaSolicitud(requestDTO.getFechaSolicitud());
        examen.setAtencionMedica(atencionMedica);

        ExamenMedico examenGuardado = examenMedicoRepositorio.save(examen);
        return mapearADTO(examenGuardado);
    }

    @Transactional
    public ExamenMedicoResponseDTO actualizarResultados(Long id, String resultados) {
        ExamenMedico examen = examenMedicoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Examen Médico no encontrado con ID: " + id));

        examen.setResultados(resultados);
        ExamenMedico examenActualizado = examenMedicoRepositorio.save(examen);

        return mapearADTO(examenActualizado);
    }

    @Transactional(readOnly = true)
    public List<ExamenMedicoResponseDTO> listarPorAtencionMedica(Long atencionMedicaId) {
        if (!atencionMedicaRepositorio.existsById(atencionMedicaId)) {
            throw new EntityNotFoundException("Atención Médica no encontrada con ID: " + atencionMedicaId);
        }
        List<ExamenMedico> examenes = examenMedicoRepositorio.findByAtencionMedicaId(atencionMedicaId);
        return examenes.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    private ExamenMedicoResponseDTO mapearADTO(ExamenMedico examen) {
        ExamenMedicoResponseDTO dto = new ExamenMedicoResponseDTO();
        dto.setId(examen.getId());
        dto.setTipoExamen(examen.getTipoExamen());
        dto.setResultados(examen.getResultados());
        dto.setFechaSolicitud(examen.getFechaSolicitud());
        dto.setAtencionMedicaId(examen.getAtencionMedica().getId());
        return dto;
    }
}
