package com.veterinaria.controladores;

import com.veterinaria.dtos.MonitoreoHospitalizacionRequestDTO;
import com.veterinaria.dtos.MonitoreoHospitalizacionResponseDTO;
import com.veterinaria.servicios.MonitoreoHospitalizacionServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoreos")
@RequiredArgsConstructor
public class MonitoreoHospitalizacionController {

    private final MonitoreoHospitalizacionServicio monitoreoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<MonitoreoHospitalizacionResponseDTO> registrarMonitoreo(@Valid @RequestBody MonitoreoHospitalizacionRequestDTO requestDTO) {
        MonitoreoHospitalizacionResponseDTO response = monitoreoServicio.agregarMonitoreo(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/hospitalizacion/{hospitalizacionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<List<MonitoreoHospitalizacionResponseDTO>> listarPorHospitalizacion(@PathVariable Long hospitalizacionId) {
        List<MonitoreoHospitalizacionResponseDTO> response = monitoreoServicio.listarPorHospitalizacion(hospitalizacionId);
        return ResponseEntity.ok(response);
    }
}
