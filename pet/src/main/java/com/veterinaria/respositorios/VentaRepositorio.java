package com.veterinaria.respositorios;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veterinaria.modelos.Venta;

public interface VentaRepositorio extends JpaRepository<Venta, Long> {

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.caja.id = :cajaId AND v.estado = com.veterinaria.modelos.Enums.EstadoVenta.ACTIVA")
    BigDecimal sumarVentasPorCaja(@Param("cajaId") Long cajaId);
}