package com.veterinaria.servicios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.dtos.CajaRequestDTO;
import com.veterinaria.modelos.CajaDiaria;
import com.veterinaria.respositorios.CajaRepositorio;
import com.veterinaria.respositorios.VentaRepositorio;

@ExtendWith(MockitoExtension.class)
class CajaServicioTest {

    @Mock
    private CajaRepositorio cajaRepositorio;

    @Mock
    private VentaRepositorio ventaRepositorio;

    @InjectMocks
    private CajaServicio cajaServicio;

    @Test
    void debeAbrirCajaYGuardarEnBaseDeDatos() {
        CajaRequestDTO request = new CajaRequestDTO();
        request.setSaldoInicial(new BigDecimal("150.00"));

        cajaServicio.abrirCaja(request);

        ArgumentCaptor<CajaDiaria> cajaCaptor = ArgumentCaptor.forClass(CajaDiaria.class);
        verify(cajaRepositorio).save(cajaCaptor.capture());

        CajaDiaria cajaGuardada = cajaCaptor.getValue();

        assertEquals(new BigDecimal("150.00"), cajaGuardada.getSaldoInicial());
        assertEquals("ABIERTA", cajaGuardada.getEstado());
        assertNotNull(cajaGuardada.getFechaApertura());
    }

    @Test
    void debeCerrarCajaYCalcularSaldoFinal() {
        // 1. Preparamos una caja ficticia abierta con 100.00
        CajaDiaria cajaAbierta = new CajaDiaria();
        cajaAbierta.setId(1L);
        cajaAbierta.setSaldoInicial(new BigDecimal("100.00"));
        cajaAbierta.setEstado("ABIERTA");
        cajaAbierta.setFechaApertura(LocalDateTime.now().minusHours(8));
        cajaAbierta.setMovimientos(new java.util.ArrayList<>());

        when(cajaRepositorio.findByEstado("ABIERTA")).thenReturn(Optional.of(cajaAbierta));

        // 2. Simulamos que hoy se vendieron 250.00
        when(ventaRepositorio.sumarVentasPorCaja(any())).thenReturn(new BigDecimal("250.00"));

        // 3. El administrador cierra la caja
        cajaServicio.cerrarCaja();

        // 4. Capturamos lo que se guardó
        ArgumentCaptor<CajaDiaria> cajaCaptor = ArgumentCaptor.forClass(CajaDiaria.class);
        verify(cajaRepositorio).save(cajaCaptor.capture());

        CajaDiaria cajaCerrada = cajaCaptor.getValue();

        // 5. Verificamos: 100 (Inicial) + 250 (Ventas) = 350
        assertEquals("CERRADA", cajaCerrada.getEstado());
        assertNotNull(cajaCerrada.getFechaCierre());
        assertEquals(new BigDecimal("350.00"), cajaCerrada.getSaldoFinal(),
                "El saldo final debe ser la suma exacta del inicial más las ventas");
    }
}