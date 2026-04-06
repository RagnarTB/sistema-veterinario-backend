package com.veterinaria.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.CajaRequestDTO;
import com.veterinaria.dtos.CierreCajaResponseDTO;
import com.veterinaria.modelos.CajaDiaria;
import com.veterinaria.modelos.Enums.TipoMovimiento;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.CajaRepositorio;
import com.veterinaria.respositorios.VentaRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;

@Service
public class CajaServicio {

    private final CajaRepositorio cajaRepositorio;
    private final VentaRepositorio ventaRepositorio;
    private final SedeRepositorio sedeRepositorio;

    public CajaServicio(CajaRepositorio cajaRepositorio, VentaRepositorio ventaRepositorio, SedeRepositorio sedeRepositorio) {
        this.cajaRepositorio = cajaRepositorio;
        this.ventaRepositorio = ventaRepositorio;
        this.sedeRepositorio = sedeRepositorio;
    }

    public void abrirCaja(CajaRequestDTO dto) {

        Sede sede = sedeRepositorio.findById(dto.getSedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));

        if (cajaRepositorio.findBySedeIdAndEstado(dto.getSedeId(), "ABIERTA").isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una caja abierta en esta sede en este momento");
        }

        CajaDiaria nuevaCaja = new CajaDiaria();
        nuevaCaja.setSaldoInicial(dto.getSaldoInicial());
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setSede(sede);

        cajaRepositorio.save(nuevaCaja);
    }

    public CierreCajaResponseDTO cerrarCaja(Long sedeId) {
        // 1. Buscar la caja abierta
        CajaDiaria cajaAbierta = cajaRepositorio.findBySedeIdAndEstado(sedeId, "ABIERTA")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay ninguna caja abierta para cerrar"));

        LocalDateTime ahora = LocalDateTime.now();
        cajaAbierta.setEstado("CERRADA");
        cajaAbierta.setFechaCierre(ahora);

        // 2. Sumar Ventas (el repositorio ya devuelve BigDecimal)
        BigDecimal totalVentas = ventaRepositorio.sumarVentasPorCaja(cajaAbierta.getId());
        totalVentas = (totalVentas == null) ? BigDecimal.ZERO : totalVentas;

        // 3. Calcular Ingresos y Egresos extras usando reduce de BigDecimal
        BigDecimal ingresosExtras = cajaAbierta.getMovimientos().stream()
                .filter(m -> m.getTipoMovimiento() == TipoMovimiento.INGRESO)
                .map(m -> m.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal egresosExtras = cajaAbierta.getMovimientos().stream()
                .filter(m -> m.getTipoMovimiento() == TipoMovimiento.EGRESO)
                .map(m -> m.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Fórmula del arqueo con BigDecimal
        BigDecimal saldoCalculado = cajaAbierta.getSaldoInicial()
                .add(totalVentas)
                .add(ingresosExtras)
                .subtract(egresosExtras);

        cajaAbierta.setSaldoFinal(saldoCalculado);

        cajaRepositorio.save(cajaAbierta);

        // 5. Retornamos el resumen detallado para el Frontend
        return new CierreCajaResponseDTO(
                cajaAbierta.getId(),
                cajaAbierta.getFechaCierre(),
                cajaAbierta.getSaldoInicial(),
                totalVentas,
                ingresosExtras,
                egresosExtras,
                saldoCalculado);
    }
}