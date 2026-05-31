package com.veterinaria.tareas;

import com.veterinaria.modelos.HistorialHospitalizacion;
import com.veterinaria.modelos.Hospitalizacion;
import com.veterinaria.respositorios.HistorialHospitalizacionRepositorio;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlertaMonitoreoJob {

    private final HospitalizacionRepositorio hospitalizacionRepositorio;
    private final HistorialHospitalizacionRepositorio historialHospitalizacionRepositorio;
    private final SimpMessagingTemplate messagingTemplate;

    // Se ejecuta cada minuto
    @Scheduled(fixedRate = 60000)
    public void verificarMonitoreosAtrasados() {
        List<Hospitalizacion> activas = hospitalizacionRepositorio.findAll().stream()
                .filter(h -> "ACTIVA".equalsIgnoreCase(h.getEstado()))
                .toList();

        LocalDateTime ahora = LocalDateTime.now();

        for (Hospitalizacion h : activas) {
            Integer frecuenciaHoras = h.getFrecuenciaMonitoreoHoras();
            if (frecuenciaHoras == null) frecuenciaHoras = 4; // Por defecto 4 horas

            List<HistorialHospitalizacion> historiales = historialHospitalizacionRepositorio
                    .findByHospitalizacionIdOrderByFechaHoraDesc(h.getId());

            LocalDateTime baseTiempo = h.getFechaIngreso();

            // Buscar el último monitoreo
            var ultimoMonitoreo = historiales.stream()
                    .filter(hist -> "MONITOREO".equalsIgnoreCase(hist.getTipoAccion()))
                    .findFirst();

            if (ultimoMonitoreo.isPresent()) {
                baseTiempo = ultimoMonitoreo.get().getFechaHora();
            }

            LocalDateTime tiempoLimite = baseTiempo.plusHours(frecuenciaHoras);

            if (ahora.isAfter(tiempoLimite)) {
                // Hay un retraso
                long minutosRetraso = java.time.Duration.between(tiempoLimite, ahora).toMinutes();

                String mensaje = String.format("El monitoreo de %s (Jaula #%s) lleva %d minutos de retraso.",
                        h.getPaciente().getNombre(), h.getJaula().getNumero(), minutosRetraso);

                // Enviar la alerta al topic de la sede o un topic global de médicos
                String topic = "/topic/alertas/sede/" + h.getJaula().getSede().getId();
                
                // Formato simple JSON
                String payload = String.format("{\"paciente\": \"%s\", \"mensaje\": \"%s\", \"hospitalizacionId\": %d}",
                        h.getPaciente().getNombre(), mensaje, h.getId());

                messagingTemplate.convertAndSend(topic, payload);
            }
        }
    }
}

