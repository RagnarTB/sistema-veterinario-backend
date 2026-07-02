package com.veterinaria.servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.veterinaria.excepciones.ResourceNotFoundException;

import com.veterinaria.dtos.ProductoRequestDTO;
import com.veterinaria.dtos.ProductoResponseDTO;
import com.veterinaria.modelos.InventarioSede;
import com.veterinaria.modelos.Producto;
import com.veterinaria.respositorios.InventarioSedeRepositorio;
import com.veterinaria.respositorios.ProductoRepositorio;
import com.veterinaria.respositorios.CategoriaProductoRepositorio;
import com.veterinaria.respositorios.UnidadMedidaRepositorio;

@Service
public class ProductoServicio {

    private final ProductoRepositorio productoRepositorio;
    private final InventarioSedeRepositorio inventarioSedeRepositorio;
    private final CategoriaProductoRepositorio categoriaRepositorio;
    private final UnidadMedidaRepositorio unidadRepositorio;

    public ProductoServicio(
            ProductoRepositorio productoRepositorio, 
            InventarioSedeRepositorio inventarioSedeRepositorio,
            CategoriaProductoRepositorio categoriaRepositorio,
            UnidadMedidaRepositorio unidadRepositorio) {
        this.productoRepositorio = productoRepositorio;
        this.inventarioSedeRepositorio = inventarioSedeRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.unidadRepositorio = unidadRepositorio;
    }

    // =========================
    // POST /api/productos
    // =========================
    public ProductoResponseDTO guardar(ProductoRequestDTO dto, Long sedeId) {
        Producto producto = new Producto();
        producto.setNombre(normalizarTexto(dto.getNombre()));
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setMarca(normalizarTexto(dto.getMarca()));
        if (dto.getFactorConversion() != null) {
            producto.setFactorConversion(dto.getFactorConversion());
        }

        if (dto.getCategoriaId() != null) {
            producto.setCategoria(categoriaRepositorio.findById(dto.getCategoriaId()).orElse(null));
        }
        if (dto.getUnidadCompraId() != null) {
            producto.setUnidadCompra(unidadRepositorio.findById(dto.getUnidadCompraId()).orElse(null));
        }
        if (dto.getUnidadVentaId() != null) {
            producto.setUnidadVenta(unidadRepositorio.findById(dto.getUnidadVentaId()).orElse(null));
        }

        // Validar coherencia entre unidades y factor de conversión
        validarUnidadesYFactor(dto.getUnidadCompraId(), dto.getUnidadVentaId(), dto.getFactorConversion());

        Producto productoGuardado = productoRepositorio.save(producto);
        return mapearAResponseDTO(productoGuardado, sedeId);
    }

    // =========================
    // GET /api/productos
    // =========================
    public Page<ProductoResponseDTO> listarTodos(String buscar, Long sedeId, Pageable pageable) {
        Page<Producto> pagina;
        if (buscar != null && !buscar.trim().isEmpty()) {
            pagina = productoRepositorio.findByNombreContainingIgnoreCase(buscar, pageable);
        } else {
            pagina = productoRepositorio.findAll(pageable);
        }
        return pagina.map(p -> this.mapearAResponseDTO(p, sedeId));
    }

    // =========================
    // GET /api/productos/{id}
    // =========================
    public ProductoResponseDTO buscarPorId(Long id, Long sedeId) {
        return productoRepositorio.findById(id)
                .map(p -> this.mapearAResponseDTO(p, sedeId))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    // =========================
    // PUT /api/productos/{id}
    // =========================
    public ProductoResponseDTO actualizar(Long id, ProductoRequestDTO dto, Long sedeId) {
        Producto productodb = productoRepositorio.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        productodb.setNombre(normalizarTexto(dto.getNombre()));
        productodb.setDescripcion(dto.getDescripcion());
        productodb.setPrecio(dto.getPrecio());
        productodb.setMarca(normalizarTexto(dto.getMarca()));
        if (dto.getFactorConversion() != null) {
            productodb.setFactorConversion(dto.getFactorConversion());
        }

        if (dto.getCategoriaId() != null) {
            productodb.setCategoria(categoriaRepositorio.findById(dto.getCategoriaId()).orElse(null));
        } else {
            productodb.setCategoria(null);
        }

        if (dto.getUnidadCompraId() != null) {
            productodb.setUnidadCompra(unidadRepositorio.findById(dto.getUnidadCompraId()).orElse(null));
        } else {
            productodb.setUnidadCompra(null);
        }

        if (dto.getUnidadVentaId() != null) {
            productodb.setUnidadVenta(unidadRepositorio.findById(dto.getUnidadVentaId()).orElse(null));
        } else {
            productodb.setUnidadVenta(null);
        }

        // Validar coherencia entre unidades y factor de conversión
        validarUnidadesYFactor(dto.getUnidadCompraId(), dto.getUnidadVentaId(), dto.getFactorConversion());

        Producto productoGuardado = productoRepositorio.save(productodb);
        return mapearAResponseDTO(productoGuardado, sedeId);
    }

    // =========================
    // PATCH /api/productos/{id}/estado
    // =========================
    public void cambiarEstado(Long id, Boolean estado) {
        Producto productodb = productoRepositorio.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        productodb.setActivo(estado);
        productoRepositorio.save(productodb);
    }

    // =========================
    // GET /api/productos/alertas-stock
    // =========================
    public List<ProductoResponseDTO> obtenerAlertasStock() {
        List<InventarioSede> alertas = inventarioSedeRepositorio.findAlertasStock();

        return alertas.stream()
                .map(inv -> this.mapearAResponseDTO(inv.getProducto(), inv.getSede().getId()))
                .collect(Collectors.toList());
    }

    // =========================
    // VALIDAR COHERENCIA UNIDADES Y FACTOR
    // =========================
    private void validarUnidadesYFactor(Long compraId, Long ventaId, java.math.BigDecimal factor) {
        if (compraId == null || ventaId == null) {
            return;
        }
        com.veterinaria.modelos.UnidadMedida compra = unidadRepositorio.findById(compraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidad de compra no válida"));
        com.veterinaria.modelos.UnidadMedida venta = unidadRepositorio.findById(ventaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidad de venta no válida"));

        // Regla 1: Unidades idénticas → factor DEBE ser 1
        if (compra.getId().equals(venta.getId())) {
            if (factor != null && factor.compareTo(java.math.BigDecimal.ONE) != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Si la unidad de compra y venta son iguales, el factor de conversión debe ser 1. No se permiten decimales ni otros valores.");
            }
            return;
        }

        String cNombre = compra.getNombre() != null ? compra.getNombre().toLowerCase() : "";
        String vNombre = venta.getNombre() != null ? venta.getNombre().toLowerCase() : "";
        String cAbrev = compra.getAbreviatura() != null ? compra.getAbreviatura().toLowerCase() : "";

        // Tipos de unidades base/continuas que NO deben convertirse a otras
        boolean isCompraBase = cNombre.contains("unidad") || cNombre.contains("und") 
                || cNombre.contains("kit") || cNombre.contains("prueba");
        boolean isCompraContinuo = cNombre.contains("mililitro") || cNombre.contains("kilogramo")
                || cNombre.contains("metro") || cAbrev.equals("ml") || cAbrev.equals("kg") || cAbrev.equals("m");

        // Regla 2: Unidades base o continuas no pueden convertirse a otras unidades distintas
        if (isCompraBase || isCompraContinuo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La unidad de compra '" + compra.getNombre() + "' no permite conversión a otras unidades. "
                    + "La unidad de compra y venta deben ser idénticas para este tipo de unidad.");
        }

        // Regla 3: Unidades granel/blíster/caja pueden convertirse, pero:
        // si la unidad de VENTA no permite decimales (es discreta), el factor debe ser entero
        boolean isCompraGranel = cNombre.contains("caja") || cNombre.contains("blíster") 
                || cNombre.contains("blister") || cNombre.contains("saco") || cNombre.contains("frasco")
                || cNombre.contains("paquete") || cNombre.contains("sobre") || cNombre.contains("ampolla");

        if (isCompraGranel) {
            if (factor == null || factor.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El factor de conversión debe ser un número positivo mayor a 0.");
            }
            // Si la unidad de venta es discreta (tableta, unidad, píldora, etc.)
            boolean ventaDiscreta = vNombre.contains("tableta") || vNombre.contains("píldora")
                    || vNombre.contains("comprimido") || vNombre.contains("cápsula")
                    || vNombre.contains("unidad") || vNombre.contains("und");
            if (ventaDiscreta) {
                // El factor debe ser entero positivo (sin decimales)
                if (factor.stripTrailingZeros().scale() > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La unidad de venta '" + venta.getNombre() + "' es discreta y no permite decimales. "
                            + "El factor de conversión debe ser un número entero positivo (ej: 12, 24, 100).");
                }
            }
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return null;
        // 1. Convertir a minúsculas
        String resultado = texto.toLowerCase();
        // 2. Quitar caracteres extraños (quedarse solo con letras, números y espacios)
        resultado = resultado.replaceAll("[^a-z0-9áéíóúñ\\s]", "");
        // 3. Eliminar espacios repetidos y trim
        resultado = resultado.replaceAll("\\s+", " ").trim();
        return resultado;
    }

    // =========================
    // MAPPER PRIVADO (evita duplicar lógica de mapeo)
    // =========================
    private ProductoResponseDTO mapearAResponseDTO(Producto producto, Long sedeId) {
        java.math.BigDecimal stockActual = java.math.BigDecimal.ZERO;
        java.math.BigDecimal stockMinimo = java.math.BigDecimal.ZERO;

        if (sedeId != null) {
            java.util.Optional<InventarioSede> inv = inventarioSedeRepositorio.findByProductoIdAndSedeId(producto.getId(), sedeId);
            if (inv.isPresent()) {
                stockActual = inv.get().getStockActual();
                stockMinimo = inv.get().getStockMinimo();
            }
        }

        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getActivo(),
                producto.getMarca(),
                producto.getCategoria() != null ? producto.getCategoria().getId() : null,
                producto.getCategoria() != null ? producto.getCategoria().getNombre() : null,
                producto.getUnidadCompra() != null ? producto.getUnidadCompra().getId() : null,
                producto.getUnidadCompra() != null ? producto.getUnidadCompra().getNombre() : null,
                producto.getUnidadVenta() != null ? producto.getUnidadVenta().getId() : null,
                producto.getUnidadVenta() != null ? producto.getUnidadVenta().getNombre() : null,
                producto.getFactorConversion(),
                stockActual,
                stockMinimo); 
    }
}
