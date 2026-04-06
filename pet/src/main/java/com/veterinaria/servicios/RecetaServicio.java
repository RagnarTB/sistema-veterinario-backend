package com.veterinaria.servicios;

import com.veterinaria.dtos.DetalleRecetaRequestDTO;
import com.veterinaria.dtos.DetalleRecetaResponseDTO;
import com.veterinaria.dtos.RecetaRequestDTO;
import com.veterinaria.dtos.RecetaResponseDTO;
import com.veterinaria.modelos.AtencionMedica;
import com.veterinaria.modelos.DetalleReceta;
import com.veterinaria.modelos.Producto;
import com.veterinaria.modelos.RecetaMedica;
import com.veterinaria.respositorios.AtencionMedicaRepositorio;
import com.veterinaria.respositorios.ProductoRepositorio;
import com.veterinaria.respositorios.RecetaRepositorio;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecetaServicio {

    private final RecetaRepositorio recetaRepositorio;
    private final AtencionMedicaRepositorio atencionMedicaRepositorio;
    private final ProductoRepositorio productoRepositorio;

    @Transactional
    public RecetaResponseDTO guardar(RecetaRequestDTO requestDTO) {
        AtencionMedica atencionMedica = atencionMedicaRepositorio.findById(requestDTO.getAtencionMedicaId())
                .orElseThrow(() -> new EntityNotFoundException("Atención Médica no encontrada con ID: " + requestDTO.getAtencionMedicaId()));

        RecetaMedica receta = new RecetaMedica();
        receta.setIndicacionesGenerales(requestDTO.getIndicacionesGenerales());
        receta.setAtencionMedica(atencionMedica);

        List<DetalleReceta> detalles = requestDTO.getDetalles().stream().map(detalleDTO -> {
            DetalleReceta detalle = new DetalleReceta();
            detalle.setMedicamento(detalleDTO.getMedicamento());
            detalle.setDosis(detalleDTO.getDosis());
            detalle.setFrecuencia(detalleDTO.getFrecuencia());
            detalle.setDuracionDias(detalleDTO.getDuracionDias());
            detalle.setRecetaMedica(receta);

            if (detalleDTO.getProductoId() != null) {
                Producto producto = productoRepositorio.findById(detalleDTO.getProductoId())
                        .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + detalleDTO.getProductoId()));
                detalle.setProducto(producto);
            }
            return detalle;
        }).collect(Collectors.toList());

        receta.setDetalles(detalles);
        RecetaMedica recetaGuardada = recetaRepositorio.save(receta);

        return mapearADTO(recetaGuardada);
    }

    @Transactional(readOnly = true)
    public RecetaResponseDTO obtenerPorAtencionMedica(Long atencionMedicaId) {
        RecetaMedica receta = recetaRepositorio.findByAtencionMedicaId(atencionMedicaId)
                .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada para la Atención Médica ID: " + atencionMedicaId));
        return mapearADTO(receta);
    }

    private RecetaResponseDTO mapearADTO(RecetaMedica receta) {
        RecetaResponseDTO dto = new RecetaResponseDTO();
        dto.setId(receta.getId());
        dto.setIndicacionesGenerales(receta.getIndicacionesGenerales());
        dto.setAtencionMedicaId(receta.getAtencionMedica().getId());

        List<DetalleRecetaResponseDTO> detallesDTO = receta.getDetalles().stream().map(detalle -> {
            DetalleRecetaResponseDTO detalleDTO = new DetalleRecetaResponseDTO();
            detalleDTO.setId(detalle.getId());
            detalleDTO.setMedicamento(detalle.getMedicamento());
            detalleDTO.setDosis(detalle.getDosis());
            detalleDTO.setFrecuencia(detalle.getFrecuencia());
            detalleDTO.setDuracionDias(detalle.getDuracionDias());
            if (detalle.getProducto() != null) {
                detalleDTO.setProductoId(detalle.getProducto().getId());
                detalleDTO.setProductoNombre(detalle.getProducto().getNombre());
            }
            return detalleDTO;
        }).collect(Collectors.toList());

        dto.setDetalles(detallesDTO);
        return dto;
    }
}
