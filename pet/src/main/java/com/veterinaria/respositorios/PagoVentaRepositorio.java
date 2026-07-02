package com.veterinaria.respositorios;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.modelos.PagoVenta;

public interface PagoVentaRepositorio extends JpaRepository<PagoVenta, Long> {

    @Query("SELECT SUM(p.monto) FROM PagoVenta p WHERE p.caja.id = :cajaId AND p.venta.estado <> com.veterinaria.modelos.Enums.EstadoVenta.ANULADA")
    BigDecimal sumarPagosPorCaja(@Param("cajaId") Long cajaId);
}
