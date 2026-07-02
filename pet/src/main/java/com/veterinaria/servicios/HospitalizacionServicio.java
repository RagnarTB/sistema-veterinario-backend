package com.veterinaria.servicios;

import com.veterinaria.dtos.HospitalizacionRequestDTO;
import com.veterinaria.dtos.HospitalizacionResponseDTO;
import com.veterinaria.modelos.Empleado;
import com.veterinaria.modelos.Hospitalizacion;
import com.veterinaria.modelos.Jaula;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.respositorios.EmpleadoRepositorio;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import com.veterinaria.respositorios.JaulaRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;
import com.veterinaria.respositorios.RangoPesoJaulaRepositorio;
import com.veterinaria.respositorios.AtencionMedicaRepositorio;
import com.veterinaria.dtos.SugerenciaJaulaResponseDTO;
import com.veterinaria.dtos.JaulaResponseDTO;
import com.veterinaria.modelos.RangoPesoJaula;
import com.veterinaria.modelos.TamanoJaula;
import com.veterinaria.modelos.Venta;
import com.veterinaria.modelos.DetalleVenta;
import com.veterinaria.respositorios.VentaRepositorio;
import com.veterinaria.respositorios.HistorialHospitalizacionRepositorio;
import com.veterinaria.respositorios.ProductoRepositorio;
import com.veterinaria.modelos.Producto;
import com.veterinaria.modelos.HistorialHospitalizacion;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.servicios.InventarioServicio;
import com.veterinaria.dtos.HistorialHospitalizacionRequestDTO;
import com.veterinaria.dtos.HistorialHospitalizacionResponseDTO;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HospitalizacionServicio {

    private final HospitalizacionRepositorio hospitalizacionRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final JaulaRepositorio jaulaRepositorio;
    private final EmpleadoRepositorio empleadoRepositorio;
    private final RangoPesoJaulaRepositorio rangoPesoJaulaRepositorio;
    private final AtencionMedicaRepositorio atencionMedicaRepositorio;
    private final VentaRepositorio ventaRepositorio;
    private final HistorialHospitalizacionRepositorio historialHospitalizacionRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final InventarioServicio inventarioServicio;
    private final ClienteRepositorio clienteRepositorio;
    @Transactional
    public HistorialHospitalizacionResponseDTO registrarMonitoreo(HistorialHospitalizacionRequestDTO req) {
        Hospitalizacion hospitalizacion = hospitalizacionRepositorio.findById(req.getHospitalizacionId())
                .orElseThrow(() -> new EntityNotFoundException("Hospitalizacion no encontrada"));

        Empleado empleado = empleadoRepositorio.findById(req.getEmpleadoId())
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));

        HistorialHospitalizacion historial = new HistorialHospitalizacion();
        historial.setHospitalizacion(hospitalizacion);
        historial.setRegistradoPor(empleado);
        historial.setFechaHora(LocalDateTime.now());
        historial.setTipoAccion(req.getTipoAccion());
        historial.setDescripcion(req.getDescripcion());

        if (req.getProductoId() != null) {
            Producto producto = productoRepositorio.findById(req.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
            historial.setProducto(producto);
            historial.setCantidadAplicada(req.getCantidadAplicada());

            if ("STOCK_CLINICA".equalsIgnoreCase(req.getOrigenMedicamento()) || "CLINICA".equalsIgnoreCase(req.getOrigenMedicamento())) {
                // Descontar de InventarioSede
                com.veterinaria.dtos.SalidaStockDTO salida = new com.veterinaria.dtos.SalidaStockDTO();
                salida.setProductoId(producto.getId());
                salida.setCantidad(req.getCantidadAplicada());
                salida.setSedeId(hospitalizacion.getJaula().getSede().getId());
                salida.setMotivo("Aplicación en hospitalización - " + hospitalizacion.getPaciente().getNombre());
                salida.setTipoMovimiento("SALIDA_CONSUMO_INTERNO");
                inventarioServicio.registrarSalidaAjuste(salida, empleado.getUsuario().getEmail());

                // Agregar costo al ticket activo
                Venta venta = ventaRepositorio.findByHospitalizacionId(hospitalizacion.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Venta de hospitalizacion no encontrada"));

                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setCantidad(req.getCantidadAplicada());
                detalle.setPrecioUnitario(producto.getPrecio());
                BigDecimal subtotal = producto.getPrecio().multiply(req.getCantidadAplicada());
                detalle.setSubtotal(subtotal);
                
                venta.agregarDetalle(detalle);
                venta.setTotal(venta.getTotal().add(subtotal));
                venta.setSaldoPendiente(venta.getSaldoPendiente().add(subtotal));
                ventaRepositorio.save(venta);
            }
        }
        historial.setOrigenMedicamento(req.getOrigenMedicamento());

        HistorialHospitalizacion guardado = historialHospitalizacionRepositorio.save(historial);

        // Actualizar proximoMonitoreo
        if (hospitalizacion.getFrecuenciaMonitoreoHoras() != null) {
            hospitalizacion.setProximoMonitoreo(LocalDateTime.now().plusHours(hospitalizacion.getFrecuenciaMonitoreoHoras()));
            hospitalizacionRepositorio.save(hospitalizacion);
        }

        HistorialHospitalizacionResponseDTO dto = new HistorialHospitalizacionResponseDTO();
        dto.setId(guardado.getId());
        dto.setHospitalizacionId(hospitalizacion.getId());
        dto.setFechaHora(guardado.getFechaHora());
        dto.setDescripcion(guardado.getDescripcion());
        dto.setTipoAccion(guardado.getTipoAccion());
        dto.setOrigenMedicamento(guardado.getOrigenMedicamento());
        if (guardado.getProducto() != null) {
            dto.setProductoId(guardado.getProducto().getId());
            dto.setProductoNombre(guardado.getProducto().getNombre());
            dto.setCantidadAplicada(guardado.getCantidadAplicada());
        }
        dto.setRegistradoPorId(empleado.getId());
        dto.setRegistradoPorNombre(empleado.getUsuario().getNombre() + " " + empleado.getUsuario().getApellido());

        return dto;
    }

    @Transactional(readOnly = true)
    public List<HospitalizacionResponseDTO> listarHospitalizacionesActivas(Long sedeId) {
        return hospitalizacionRepositorio.findAll().stream()
                .filter(h -> h.getEstado().equals("ACTIVA"))
                .filter(h -> sedeId == null || h.getJaula().getSede().getId().equals(sedeId))
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public SugerenciaJaulaResponseDTO sugerirJaula(Long pacienteId, Long sedeId, BigDecimal pesoActual) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));

        BigDecimal ultimoPeso = BigDecimal.ZERO;
        if (pesoActual != null) {
            ultimoPeso = pesoActual;
        } else {
            var ultimaAtencion = atencionMedicaRepositorio.findTopByPacienteIdOrderByFechaCreacionDesc(pacienteId);
            if (ultimaAtencion.isPresent() && ultimaAtencion.get().getPeso() != null) {
                ultimoPeso = ultimaAtencion.get().getPeso();
            }
        }

        SugerenciaJaulaResponseDTO response = new SugerenciaJaulaResponseDTO();
        response.setPacienteId(paciente.getId());
        response.setPacienteNombre(paciente.getNombre());
        response.setEspecieNombre(paciente.getEspecie().getNombre());
        response.setUltimoPeso(ultimoPeso);

        // Buscar rango de peso
        List<RangoPesoJaula> rangos = rangoPesoJaulaRepositorio.findByEspecieId(paciente.getEspecie().getId());
        TamanoJaula tamanoSugerido = null;
        for (RangoPesoJaula rango : rangos) {
            if (ultimoPeso.compareTo(rango.getPesoMinimo()) >= 0 && ultimoPeso.compareTo(rango.getPesoMaximo()) <= 0) {
                tamanoSugerido = rango.getTamanoJaula();
                break;
            }
        }

        if (tamanoSugerido != null) {
            response.setTamanoSugeridoId(tamanoSugerido.getId());
            response.setTamanoSugeridoNombre(tamanoSugerido.getNombre());
        }

        // Buscar jaulas disponibles en la sede
        List<Jaula> disponibles = jaulaRepositorio.findBySedeIdAndEstadoAndActivoTrue(sedeId, "DISPONIBLE");

        // Mapear jaulas a DTO
        List<JaulaResponseDTO> jaulasDTO = disponibles.stream().map(j -> {
            JaulaResponseDTO dto = new JaulaResponseDTO();
            dto.setId(j.getId());
            dto.setNumero(j.getNumero());
            dto.setEstado(j.getEstado());
            dto.setActivo(j.getActivo());
            dto.setSedeId(j.getSede().getId());
            if (j.getCategoria() != null) {
                dto.setCategoriaNombre(j.getCategoria().getNombre());
            }
            if (j.getTamano() != null) {
                dto.setTamanoNombre(j.getTamano().getNombre());
                dto.setTamanoId(j.getTamano().getId());
            }
            dto.setAlertaContagio(j.getAlertaContagio());
            return dto;
        }).collect(Collectors.toList());

        response.setJaulasDisponibles(jaulasDTO);

        return response;
    }

    @Transactional
    public HospitalizacionResponseDTO ingresarPaciente(HospitalizacionRequestDTO requestDTO) {
        // Verificar si el paciente ya tiene una hospitalizacion activa
        hospitalizacionRepositorio.findByPacienteIdAndEstado(requestDTO.getPacienteId(), "ACTIVA")
                .ifPresent(h -> {
                    throw new IllegalStateException(
                            "El paciente ya se encuentra hospitalizado (Hospitalizacion ACTIVA).");
                });

        Paciente paciente = pacienteRepositorio.findById(requestDTO.getPacienteId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Paciente no encontrado con ID: " + requestDTO.getPacienteId()));

        Jaula jaula = jaulaRepositorio.findById(requestDTO.getJaulaId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Jaula no encontrada con ID: " + requestDTO.getJaulaId()));

        if (!"DISPONIBLE".equalsIgnoreCase(jaula.getEstado())) {
            throw new IllegalStateException("La jaula solicitada no esta DISPONIBLE.");
        }

        Empleado empleado = empleadoRepositorio.findById(requestDTO.getEmpleadoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Empleado (veterinario) no encontrado con ID: " + requestDTO.getEmpleadoId()));

        Hospitalizacion hospitalizacion = new Hospitalizacion();
        hospitalizacion.setMotivoIngreso(requestDTO.getMotivoIngreso());
        hospitalizacion.setFechaIngreso(requestDTO.getFechaIngreso() != null ? requestDTO.getFechaIngreso() : LocalDateTime.now());
        hospitalizacion.setEstado("ACTIVA");
        hospitalizacion.setFrecuenciaMonitoreoHoras(requestDTO.getFrecuenciaMonitoreoHoras() != null ? requestDTO.getFrecuenciaMonitoreoHoras() : 4);
        hospitalizacion.setNivelGravedad("ESTABLE");
        hospitalizacion.setProximoMonitoreo(hospitalizacion.getFechaIngreso().plusHours(hospitalizacion.getFrecuenciaMonitoreoHoras()));
        hospitalizacion.setPaciente(paciente);
        hospitalizacion.setJaula(jaula);
        hospitalizacion.setEmpleado(empleado);

        // Cambiar estado de la jaula
        jaula.setEstado("OCUPADA");
        jaulaRepositorio.save(jaula);

        Hospitalizacion guardada = hospitalizacionRepositorio.save(hospitalizacion);

        // Crear ticket "EN_CURSO" (Venta ACTIVA) al momento del ingreso
        Venta ventaServicio = new Venta();
        ventaServicio.setCliente(paciente.getCliente());
        ventaServicio.setFechaHora(LocalDateTime.now());
        ventaServicio.setEstado(com.veterinaria.modelos.Enums.EstadoVenta.ACTIVA);
        ventaServicio.setTipoComprobante(com.veterinaria.modelos.Enums.TipoComprobante.BOLETA);
        ventaServicio.setTotal(BigDecimal.ZERO);
        ventaServicio.setMontoPagado(BigDecimal.ZERO);
        ventaServicio.setSaldoPendiente(BigDecimal.ZERO);
        ventaServicio.setHospitalizacion(guardada);
        ventaRepositorio.save(ventaServicio);

        // Guardar peso si se proporciona actualizando/creando AtencionMedica
        if (requestDTO.getPesoActual() != null && requestDTO.getPesoActual().compareTo(BigDecimal.ZERO) > 0) {
            com.veterinaria.modelos.AtencionMedica atencion = new com.veterinaria.modelos.AtencionMedica();
            atencion.setPaciente(paciente);
            atencion.setVeterinario(empleado);
            atencion.setDiagnostico("Ingreso a hospitalización");
            atencion.setPeso(requestDTO.getPesoActual());
            atencion.setActivo(true);
            atencionMedicaRepositorio.save(atencion);
        }

        return mapearADTO(guardada);
    }

    @Transactional
    public HospitalizacionResponseDTO darDeAlta(Long hospitalizacionId) {
        Hospitalizacion hospitalizacion = hospitalizacionRepositorio.findById(hospitalizacionId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Hospitalizacion no encontrada con ID: " + hospitalizacionId));

        if (!"ACTIVA".equalsIgnoreCase(hospitalizacion.getEstado())) {
            throw new IllegalStateException("La hospitalizacion no esta ACTIVA.");
        }

        LocalDateTime fechaAlta = LocalDateTime.now();
        hospitalizacion.setFechaAlta(fechaAlta);
        hospitalizacion.setEstado("DADA_DE_ALTA");

        Jaula jaula = hospitalizacion.getJaula();

        // Calcular dias de hospitalizacion
        long dias = ChronoUnit.DAYS.between(hospitalizacion.getFechaIngreso(), fechaAlta);
        if (dias == 0)
            dias = 1; // Minimo 1 dia a cobrar

        BigDecimal tarifaDiaria = (jaula.getCategoria() != null && jaula.getCategoria().getPrecioPorDia() != null)
                ? jaula.getCategoria().getPrecioPorDia()
                : BigDecimal.ZERO;

        BigDecimal totalDias = tarifaDiaria.multiply(BigDecimal.valueOf(dias));

        BigDecimal costoInsumos = BigDecimal.ZERO;
        BigDecimal totalGeneral = totalDias.add(costoInsumos);

        Venta venta = ventaRepositorio.findByHospitalizacionId(hospitalizacion.getId()).orElse(null);

        if (venta != null) {
            if (totalDias.compareTo(BigDecimal.ZERO) > 0) {
                DetalleVenta detalle = new DetalleVenta();
                detalle.setCantidad(BigDecimal.valueOf(dias));
                detalle.setPrecioUnitario(tarifaDiaria);
                detalle.setSubtotal(totalDias);
                venta.agregarDetalle(detalle);

                venta.setTotal(venta.getTotal().add(totalDias));
                venta.setSaldoPendiente(venta.getSaldoPendiente().add(totalDias));
                ventaRepositorio.save(venta);
            }

            // Traspasar deuda acumulada al cliente si el saldo final queda pendiente
            com.veterinaria.modelos.Cliente cliente = venta.getCliente();
            if (cliente != null && venta.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
                if (cliente.getDeudaAcumulada() == null) {
                    cliente.setDeudaAcumulada(BigDecimal.ZERO);
                }
                cliente.setDeudaAcumulada(cliente.getDeudaAcumulada().add(venta.getSaldoPendiente()));
                clienteRepositorio.save(cliente);
            }
        }

        // Estado transicional: Desinfeccion
        jaula.setEstado("EN_DESINFECCION");
        jaulaRepositorio.save(jaula);

        Hospitalizacion actualizada = hospitalizacionRepositorio.save(hospitalizacion);
        return mapearADTO(actualizada);
    }

    @Transactional
    public HospitalizacionResponseDTO trasladarPaciente(Long hospitalizacionId, Long nuevaJaulaId) {
        Hospitalizacion hospitalizacion = hospitalizacionRepositorio.findById(hospitalizacionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Hospitalizacion no encontrada con ID: " + hospitalizacionId));

        if (!"ACTIVA".equalsIgnoreCase(hospitalizacion.getEstado())) {
            throw new IllegalStateException("Solo puede trasladar a pacientes con hospitalizacion ACTIVA.");
        }

        Jaula nuevaJaula = jaulaRepositorio.findById(nuevaJaulaId)
                .orElseThrow(() -> new EntityNotFoundException("Jaula no encontrada con ID: " + nuevaJaulaId));

        if (!"DISPONIBLE".equalsIgnoreCase(nuevaJaula.getEstado())) {
            throw new IllegalStateException("La nueva jaula solicitada no esta DISPONIBLE.");
        }

        // Liberar jaula actual
        Jaula jaulaActual = hospitalizacion.getJaula();
        jaulaActual.setEstado("EN_DESINFECCION");
        jaulaRepositorio.save(jaulaActual);

        // Ocupar nueva jaula
        nuevaJaula.setEstado("OCUPADA");
        jaulaRepositorio.save(nuevaJaula);

        // Actualizar hospitalizacion
        hospitalizacion.setJaula(nuevaJaula);
        Hospitalizacion actualizada = hospitalizacionRepositorio.save(hospitalizacion);
        return mapearADTO(actualizada);
    }

    @Transactional
    public HospitalizacionResponseDTO registrarFallecimiento(Long hospitalizacionId) {
        Hospitalizacion hospitalizacion = hospitalizacionRepositorio.findById(hospitalizacionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Hospitalizacion no encontrada con ID: " + hospitalizacionId));

        if (!"ACTIVA".equalsIgnoreCase(hospitalizacion.getEstado())) {
            throw new IllegalStateException("La hospitalizacion no esta ACTIVA.");
        }

        hospitalizacion.setFechaAlta(LocalDateTime.now());
        hospitalizacion.setEstado("FALLECIDO");

        Jaula jaula = hospitalizacion.getJaula();
        jaula.setEstado("EN_DESINFECCION");
        jaulaRepositorio.save(jaula);

        Hospitalizacion actualizada = hospitalizacionRepositorio.save(hospitalizacion);
        return mapearADTO(actualizada);
    }

    @Transactional
    public HospitalizacionResponseDTO cambiarGravedad(Long hospitalizacionId, String nuevoNivel) {
        Hospitalizacion hospitalizacion = hospitalizacionRepositorio.findById(hospitalizacionId)
                .orElseThrow(() -> new EntityNotFoundException("Hospitalizacion no encontrada con ID: " + hospitalizacionId));
        
        if (!"ESTABLE".equals(nuevoNivel) && !"OBSERVACION".equals(nuevoNivel) && !"CRITICO".equals(nuevoNivel)) {
            throw new IllegalArgumentException("Nivel de gravedad inválido.");
        }

        hospitalizacion.setNivelGravedad(nuevoNivel);
        return mapearADTO(hospitalizacionRepositorio.save(hospitalizacion));
    }

    @Transactional(readOnly = true)
    public List<HistorialHospitalizacionResponseDTO> listarHistorial(Long hospitalizacionId) {
        return historialHospitalizacionRepositorio.findAll().stream()
                .filter(h -> h.getHospitalizacion().getId().equals(hospitalizacionId))
                .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()))
                .map(this::mapearHistorialADTO)
                .collect(Collectors.toList());
    }

    private HistorialHospitalizacionResponseDTO mapearHistorialADTO(HistorialHospitalizacion guardado) {
        HistorialHospitalizacionResponseDTO dto = new HistorialHospitalizacionResponseDTO();
        dto.setId(guardado.getId());
        dto.setHospitalizacionId(guardado.getHospitalizacion().getId());
        dto.setFechaHora(guardado.getFechaHora());
        dto.setDescripcion(guardado.getDescripcion());
        dto.setTipoAccion(guardado.getTipoAccion());
        dto.setOrigenMedicamento(guardado.getOrigenMedicamento());
        if (guardado.getProducto() != null) {
            dto.setProductoId(guardado.getProducto().getId());
            dto.setProductoNombre(guardado.getProducto().getNombre());
            dto.setCantidadAplicada(guardado.getCantidadAplicada());
        }
        if (guardado.getRegistradoPor() != null) {
            dto.setRegistradoPorId(guardado.getRegistradoPor().getId());
            dto.setRegistradoPorNombre(guardado.getRegistradoPor().getUsuario().getNombre() + " " + guardado.getRegistradoPor().getUsuario().getApellido());
        }
        return dto;
    }

    private HospitalizacionResponseDTO mapearADTO(Hospitalizacion hospitalizacion) {
        HospitalizacionResponseDTO dto = new HospitalizacionResponseDTO();
        dto.setId(hospitalizacion.getId());
        dto.setMotivoIngreso(hospitalizacion.getMotivoIngreso());
        dto.setFechaIngreso(hospitalizacion.getFechaIngreso());
        dto.setFechaAlta(hospitalizacion.getFechaAlta());
        dto.setEstado(hospitalizacion.getEstado());
        dto.setNivelGravedad(hospitalizacion.getNivelGravedad());
        dto.setProximoMonitoreo(hospitalizacion.getProximoMonitoreo());
        if (hospitalizacion.getProximoMonitoreo() != null) {
            dto.setMonitoreoAtrasado(LocalDateTime.now().isAfter(hospitalizacion.getProximoMonitoreo()));
        } else {
            dto.setMonitoreoAtrasado(false);
        }
        dto.setFrecuenciaMonitoreoHoras(hospitalizacion.getFrecuenciaMonitoreoHoras());
        dto.setPacienteId(hospitalizacion.getPaciente().getId());
        dto.setPacienteNombre(hospitalizacion.getPaciente().getNombre());
        dto.setPacienteEspecie(hospitalizacion.getPaciente().getEspecie() != null ? hospitalizacion.getPaciente().getEspecie().getNombre() : "Desconocida");
        dto.setJaulaId(hospitalizacion.getJaula().getId());
        dto.setJaulaNumero(hospitalizacion.getJaula().getNumero());
        if (hospitalizacion.getEmpleado() != null) {
            dto.setEmpleadoId(hospitalizacion.getEmpleado().getId());
        }
        if (hospitalizacion.getEmpleado() != null && hospitalizacion.getEmpleado().getUsuario() != null) {
            dto.setEmpleadoNombre(hospitalizacion.getEmpleado().getUsuario().getNombre() + " "
                    + hospitalizacion.getEmpleado().getUsuario().getApellido());
        }
        return dto;
    }
}

