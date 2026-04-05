package com.veterinaria.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.DetalleVentaRequestDTO;
import com.veterinaria.dtos.DetalleVentaResponseDTO;
import com.veterinaria.dtos.MensajeResponseDTO;
import com.veterinaria.dtos.VentaRequestDTO;
import com.veterinaria.dtos.VentaResponseDTO;
import com.veterinaria.modelos.CajaDiaria;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.DetalleVenta;
import com.veterinaria.modelos.MovimientoCaja;
import com.veterinaria.modelos.Producto;
import com.veterinaria.modelos.Venta;
import com.veterinaria.modelos.Enums.EstadoVenta;
import com.veterinaria.modelos.Enums.TipoMovimiento;
import com.veterinaria.respositorios.CajaRepositorio;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.respositorios.MovimientoCajaRespositorio;
import com.veterinaria.respositorios.ProductoRepositorio;
import com.veterinaria.respositorios.VentaRepositorio;

import jakarta.transaction.Transactional;

@Service
public class VentaServicio {

        private final VentaRepositorio ventaRepositorio;
        private final ClienteRepositorio clienteRepositorio;
        private final ProductoRepositorio productoRepositorio;
        private final CajaRepositorio cajaRepositorio;
        private final MovimientoCajaRespositorio movimientoCajaRespositorio;

        public VentaServicio(VentaRepositorio ventaRepositorio, ClienteRepositorio clienteRepositorio,
                        ProductoRepositorio productoRepositorio, CajaRepositorio cajaRepositorio,
                        MovimientoCajaRespositorio movimientoCajaRespositorio) {
                this.ventaRepositorio = ventaRepositorio;
                this.clienteRepositorio = clienteRepositorio;
                this.productoRepositorio = productoRepositorio;
                this.cajaRepositorio = cajaRepositorio;
                this.movimientoCajaRespositorio = movimientoCajaRespositorio;
        }

        @Transactional
        public VentaResponseDTO guardar(VentaRequestDTO dto) {

                // ------------------------------------------------------------------
                // REGLA DE NEGOCIO: No se puede vender con caja cerrada
                // ------------------------------------------------------------------
                CajaDiaria cajaAbierta = cajaRepositorio.findByEstado("ABIERTA")
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "No se puede realizar la venta porque la caja está cerrada"));

                Cliente cliente = clienteRepositorio.findById(dto.getClienteId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Cliente no encontrado con id: " + dto.getClienteId()));

                Venta venta = new Venta();
                venta.setCliente(cliente);
                venta.setFechaHora(LocalDateTime.now());
                venta.setCaja(cajaAbierta);

                BigDecimal totalVenta = BigDecimal.ZERO;

                for (DetalleVentaRequestDTO detalleDto : dto.getDetalles()) {

                        Producto producto = productoRepositorio.findById(detalleDto.getProductoId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND,
                                                        "Producto no encontrado con id: "
                                                                        + detalleDto.getProductoId()));

                        if (!producto.getActivo()) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "El producto '" + producto.getNombre()
                                                                + "' se encuentra inactivo y no puede ser vendido.");
                        }

                        if (producto.getStockActual() < detalleDto.getCantidad()) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "No hay stock suficiente para: " + producto.getNombre());
                        }

                        producto.setStockActual(
                                        producto.getStockActual() - detalleDto.getCantidad());

                        // Multiplicación con BigDecimal: precio * cantidad
                        BigDecimal subtotal = producto.getPrecio()
                                        .multiply(BigDecimal.valueOf(detalleDto.getCantidad()));
                        totalVenta = totalVenta.add(subtotal);

                        DetalleVenta nuevoDetalle = new DetalleVenta();
                        nuevoDetalle.setProducto(producto);
                        nuevoDetalle.setCantidad(detalleDto.getCantidad());
                        nuevoDetalle.setPrecioUnitario(producto.getPrecio());
                        nuevoDetalle.setSubtotal(subtotal);

                        venta.agregarDetalle(nuevoDetalle);
                }

                venta.setTotal(totalVenta);
                venta.setEstado(EstadoVenta.ACTIVA);

                Venta ventaGuardada = ventaRepositorio.save(venta);

                return mapearAVentaResponseDTO(ventaGuardada);
        }

        // =========================
        // GET /api/ventas
        // =========================
        public Page<VentaResponseDTO> listarTodas(Pageable pageable) {
                return ventaRepositorio.findAll(pageable)
                                .map(this::mapearAVentaResponseDTO);
        }

        // =========================
        // GET /api/ventas/{id}
        // =========================
        public VentaResponseDTO buscarPorId(Long id) {
                Venta venta = ventaRepositorio.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Venta no encontrada con id: " + id));

                return mapearAVentaResponseDTO(venta);
        }

        @Transactional
        public MensajeResponseDTO anularVenta(Long idVenta) {
                // 1. Buscar la venta
                Venta venta = ventaRepositorio.findById(idVenta)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Venta no encontrada"));

                // Validación: No anular lo ya anulado
                if (venta.getEstado() == EstadoVenta.ANULADA) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta ya está anulada");
                }

                // 2. Buscar la Caja Abierta actual
                CajaDiaria cajaAbierta = cajaRepositorio.findByEstado("ABIERTA")
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "No se puede anular: No hay una caja abierta para registrar el egreso"));

                // --- VALIDACIÓN DE SALDO ---
                // El repositorio ya devuelve BigDecimal; si no hay ventas, retorna null
                BigDecimal ventasActuales = ventaRepositorio.sumarVentasPorCaja(cajaAbierta.getId());
                ventasActuales = (ventasActuales == null) ? BigDecimal.ZERO : ventasActuales;

                // Comparación correcta con BigDecimal: compareTo < 0 significa "menor que"
                if (ventasActuales.compareTo(venta.getTotal()) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "No hay suficiente efectivo en caja para devolver el monto de esta venta.");
                }

                // 3. Devolver stock
                for (DetalleVenta detalle : venta.getDetalles()) {
                        Producto producto = detalle.getProducto();
                        producto.setStockActual(producto.getStockActual() + detalle.getCantidad());
                        productoRepositorio.save(producto);
                }

                // 4. Registrar el Movimiento de Caja (EGRESO)
                MovimientoCaja egreso = new MovimientoCaja();
                egreso.setConcepto("Anulación de Venta ID: " + venta.getId());
                egreso.setMonto(venta.getTotal());
                egreso.setTipoMovimiento(TipoMovimiento.EGRESO);
                egreso.setFechaHora(LocalDateTime.now());
                egreso.setCajaDiaria(cajaAbierta);

                movimientoCajaRespositorio.save(egreso);

                // 5. Cambiar el estado
                venta.setEstado(EstadoVenta.ANULADA);
                ventaRepositorio.save(venta);

                return new MensajeResponseDTO("Venta anulada correctamente y stock devuelto");
        }

        // =========================
        // MAPPER PRIVADO
        // =========================
        private VentaResponseDTO mapearAVentaResponseDTO(Venta venta) {
                VentaResponseDTO respuesta = new VentaResponseDTO();
                respuesta.setId(venta.getId());
                respuesta.setClienteId(venta.getCliente().getId());
                respuesta.setFechaHora(venta.getFechaHora());
                respuesta.setTotal(venta.getTotal());

                List<DetalleVentaResponseDTO> detallesDTO = venta.getDetalles()
                                .stream()
                                .map(detalle -> new DetalleVentaResponseDTO(
                                                detalle.getProducto().getId(),
                                                detalle.getProducto().getNombre(),
                                                detalle.getCantidad(),
                                                detalle.getPrecioUnitario(),
                                                detalle.getSubtotal()))
                                .collect(Collectors.toList());
                respuesta.setDetalles(detallesDTO);

                return respuesta;
        }
}