package com.veterinaria.servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.veterinaria.dtos.CitasVeterinarioDTO;
import com.veterinaria.dtos.DashboardResumenDTO;
import com.veterinaria.dtos.TopProductoDTO;
import com.veterinaria.respositorios.CitaRepositorio;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.respositorios.DetalleVentaRepositorio;
import com.veterinaria.respositorios.VentaRepositorio;

@Service
public class ReporteServicio {

    private final DetalleVentaRepositorio detalleVentaRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final VentaRepositorio ventaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final com.veterinaria.respositorios.MovimientoCajaRespositorio movimientoCajaRespositorio;

    public ReporteServicio(DetalleVentaRepositorio detalleVentaRepositorio,
            CitaRepositorio citaRepositorio,
            VentaRepositorio ventaRepositorio,
            ClienteRepositorio clienteRepositorio,
            com.veterinaria.respositorios.MovimientoCajaRespositorio movimientoCajaRespositorio) {
        this.detalleVentaRepositorio = detalleVentaRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.ventaRepositorio = ventaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.movimientoCajaRespositorio = movimientoCajaRespositorio;
    }

    // Top 5 productos más vendidos (ventas activas)
    public List<TopProductoDTO> obtenerTopProductos() {
        return detalleVentaRepositorio.findTopProductos(PageRequest.of(0, 5));
    }

    // Rendimiento de veterinarios por citas completadas
    public List<CitasVeterinarioDTO> obtenerRendimientoVeterinarios() {
        return citaRepositorio.contarCitasCompletadasPorVeterinario();
    }

    // Resumen para el dashboard principal
    public DashboardResumenDTO obtenerResumenDashboard() {
        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        int mes = hoy.getMonthValue();

        // Calcular flujo neto a partir del repositorio de movimientos
        BigDecimal flujoNetoMes = movimientoCajaRespositorio.calcularFlujoNetoMes(anio, mes);
        if (flujoNetoMes == null) {
            flujoNetoMes = BigDecimal.ZERO;
        }

        long totalClientesActivos = clienteRepositorio.countByActivoTrue();

        return new DashboardResumenDTO(flujoNetoMes, totalClientesActivos);
    }
}
