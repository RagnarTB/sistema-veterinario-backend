package com.veterinaria.servicios;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.veterinaria.dtos.InventarioRequestDTO;
import com.veterinaria.modelos.InventarioSede;
import com.veterinaria.modelos.Producto;
import com.veterinaria.modelos.Sede;
import com.veterinaria.respositorios.InventarioSedeRepositorio;
import com.veterinaria.respositorios.ProductoRepositorio;
import com.veterinaria.respositorios.SedeRepositorio;

@Service
public class InventarioServicio {

    private final InventarioSedeRepositorio inventarioSedeRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final SedeRepositorio sedeRepositorio;

    public InventarioServicio(InventarioSedeRepositorio inventarioSedeRepositorio, ProductoRepositorio productoRepositorio, SedeRepositorio sedeRepositorio) {
        this.inventarioSedeRepositorio = inventarioSedeRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.sedeRepositorio = sedeRepositorio;
    }

    @Transactional
    public InventarioSede actualizarInventario(InventarioRequestDTO dto) {
        Producto producto = productoRepositorio.findById(dto.getProductoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        Sede sede = sedeRepositorio.findById(dto.getSedeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));

        InventarioSede inventario = inventarioSedeRepositorio.findByProductoIdAndSedeId(dto.getProductoId(), dto.getSedeId())
                .orElse(new InventarioSede());

        inventario.setProducto(producto);
        inventario.setSede(sede);
        inventario.setStockActual(dto.getStockActual());
        inventario.setStockMinimo(dto.getStockMinimo());

        return inventarioSedeRepositorio.save(inventario);
    }
}
