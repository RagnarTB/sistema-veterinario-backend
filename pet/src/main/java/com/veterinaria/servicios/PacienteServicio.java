package com.veterinaria.servicios;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.veterinaria.dtos.PacienteRequestDTO;
import com.veterinaria.dtos.PacienteResponseDTO;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.Especie;
import com.veterinaria.modelos.Paciente;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.respositorios.EspecieRepositorio;
import com.veterinaria.respositorios.PacienteRepositorio;

@Service
public class PacienteServicio {

        private final PacienteRepositorio pacienteRepositorio;
        private final ClienteRepositorio clienteRepositorio;
        private final EspecieRepositorio especieRepositorio;
        private final UsuarioAutenticadoService usuarioAutenticadoService;

        public PacienteServicio(PacienteRepositorio pacienteRepositorio,
                        ClienteRepositorio clienteRepositorio,
                        EspecieRepositorio especieRepositorio,
                        UsuarioAutenticadoService usuarioAutenticadoService) {
                this.pacienteRepositorio = pacienteRepositorio;
                this.clienteRepositorio = clienteRepositorio;
                this.especieRepositorio = especieRepositorio;
                this.usuarioAutenticadoService = usuarioAutenticadoService;
        }

        public PacienteResponseDTO guardar(PacienteRequestDTO dto) {
                if (usuarioAutenticadoService.esCliente()) {
                        Long clienteActualId = usuarioAutenticadoService.obtenerClienteActual().getId();
                        dto.setClienteId(clienteActualId);
                } else if (dto.getClienteId() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del cliente es obligatorio.");
                }

                Cliente dueno = clienteRepositorio.findById(dto.getClienteId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Cliente no encontrado con ID: " + dto.getClienteId()));

                Especie especie = especieRepositorio.findById(dto.getEspecieId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Especie no encontrada con id: " + dto.getEspecieId()));

                Paciente paciente = new Paciente();
                paciente.setNombre(dto.getNombre() != null ? dto.getNombre().toUpperCase().trim() : null);
                paciente.setEspecie(especie);
                paciente.setRaza(dto.getRaza());
                paciente.setSexo(dto.getSexo());
                paciente.setFechaNacimiento(dto.getFechaNacimiento());
                paciente.setCliente(dueno);

                Paciente pacienteGuardado = pacienteRepositorio.save(paciente);

                // Usamos nuestro método ayudante
                return mapearAResponse(pacienteGuardado);
        }

        public Page<PacienteResponseDTO> listarTodos(String buscar, Boolean estado, Pageable pageable) {
                Page<Paciente> pagina;

                if (usuarioAutenticadoService.esCliente()) {
                        Long clienteId = usuarioAutenticadoService.obtenerClienteActual().getId();
                        if (buscar != null && !buscar.trim().isEmpty()) {
                                pagina = pacienteRepositorio.buscarPacientesPorCliente(clienteId, buscar, estado, pageable);
                        } else {
                                pagina = pacienteRepositorio.findAllPorClienteConFiltro(clienteId, estado, pageable);
                        }
                } else {
                        if (buscar != null && !buscar.trim().isEmpty()) {
                                pagina = pacienteRepositorio.buscarPacientes(buscar, estado, pageable);
                        } else {
                                pagina = pacienteRepositorio.findAllConFiltro(estado, pageable);
                        }
                }

                return pagina.map(this::mapearAResponse);
        }

        public PacienteResponseDTO buscarPorId(Long id) {
                Paciente pacienteDb = pacienteRepositorio.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Paciente no encontrado con ID: " + id));

                if (usuarioAutenticadoService.esCliente()) {
                        Long clienteActualId = usuarioAutenticadoService.obtenerClienteActual().getId();
                        if (!pacienteDb.getCliente().getId().equals(clienteActualId)) {
                                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No tienes permiso para ver este paciente.");
                        }
                }

                return mapearAResponse(pacienteDb);
        }

        public PacienteResponseDTO actualizar(Long id, PacienteRequestDTO dto) {
                Paciente pacienteDb = pacienteRepositorio.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Paciente no encontrado con ID: " + id));

                if (usuarioAutenticadoService.esCliente()) {
                        Long clienteActualId = usuarioAutenticadoService.obtenerClienteActual().getId();
                        if (!pacienteDb.getCliente().getId().equals(clienteActualId)) {
                                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No tienes permiso para actualizar este paciente.");
                        }
                }

                Cliente dueno = clienteRepositorio.findById(dto.getClienteId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Cliente no encontrado con ID: " + dto.getClienteId()));

                // 2. CORRECCIÓN: Buscamos la Especie en BD antes de actualizar
                Especie especie = especieRepositorio.findById(dto.getEspecieId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Especie no encontrada con id: " + dto.getEspecieId()));

                pacienteDb.setNombre(dto.getNombre() != null ? dto.getNombre().toUpperCase().trim() : null);
                pacienteDb.setEspecie(especie); // Asignamos el objeto Especie
                pacienteDb.setRaza(dto.getRaza());
                pacienteDb.setSexo(dto.getSexo());
                pacienteDb.setFechaNacimiento(dto.getFechaNacimiento());
                pacienteDb.setCliente(dueno);

                Paciente pacienteGuardado = pacienteRepositorio.save(pacienteDb);

                return mapearAResponse(pacienteGuardado);
        }

        public void cambiarEstado(Long id, Boolean estado) {
                Paciente pacienteDb = pacienteRepositorio.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Paciente no encontrado con ID: " + id));
                pacienteDb.setActivo(estado);
                pacienteRepositorio.save(pacienteDb);
        }

        // --- 3. CORRECCIÓN: EL MÉTODO AYUDANTE (Refactorización Limpia) ---
        private PacienteResponseDTO mapearAResponse(Paciente paciente) {
                return new PacienteResponseDTO(
                                paciente.getId(),
                                paciente.getNombre(),
                                paciente.getEspecie().getNombre(), // Extraemos el nombre de la especie
                                paciente.getRaza(),
                                paciente.getSexo(),
                                paciente.getFechaNacimiento(),
                                paciente.getCliente().getId(),
                                paciente.getCliente().getUsuario().getNombre() + " " + paciente.getCliente().getUsuario().getApellido(),
                                paciente.getActivo());
        }
}