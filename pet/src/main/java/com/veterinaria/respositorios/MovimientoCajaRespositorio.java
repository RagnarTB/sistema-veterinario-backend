package com.veterinaria.respositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.modelos.MovimientoCaja;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoCajaRespositorio extends JpaRepository<MovimientoCaja, Long> {

    @Query("SELECT SUM(CASE WHEN m.tipoMovimiento = com.veterinaria.modelos.Enums.TipoMovimiento.INGRESO THEN m.monto ELSE -m.monto END) " +
           "FROM MovimientoCaja m " +
           "WHERE YEAR(m.fechaHora) = :anio AND MONTH(m.fechaHora) = :mes")
    BigDecimal calcularFlujoNetoMes(@Param("anio") int anio, @Param("mes") int mes);

}
