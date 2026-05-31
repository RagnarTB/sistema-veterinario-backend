package com.veterinaria.servicios;

import com.veterinaria.modelos.Hospitalizacion;
import com.veterinaria.modelos.MonitoreoHospitalizacion;
import com.veterinaria.respositorios.HospitalizacionRepositorio;
import com.veterinaria.respositorios.MonitoreoHospitalizacionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoreoNotificacionServicio {

    private final HospitalizacionRepositorio hospitalizacionRepositorio;
    private final MonitoreoHospitalizacionRepositorio monitoreoRepositorio;
    private final SimpMessagingTemplate messagingTemplate;

    // Se ejecuta cada minuto
    @Scheduled(fixedRate = 60000)
    public void verificarRetrasosDeMonitoreo() {
        List<Hospitalizacion> activas = hospitalizacionRepositorio.findByEstado("ACTIVA");

        LocalDateTime ahora = LocalDateTime.now();

        for (Hospitalizacion h : activas) {
            Integer frecuencia = h.getFrecuenciaMonitoreoHoras();
            if (frecuencia == null || frecuencia <= 0) continue;

            // Obtener el ǧltimo monitoreo
            Optional<MonitoreoHospitalizacion> ultimoMonitoreoOpt = monitoreoRepositorio
                    .findTopByHospitalizacionIdOrderByFechaHoraDesc(h.getId());

            LocalDateTime referencia = h.getFechaIngreso();
            if (ultimoMonitoreoOpt.isPresent()) {
                referencia = ultimoMonitoreoOpt.get().getFechaHora();
            }

            LocalDateTime limite = referencia.plusHours(frecuencia);

            if (ahora.isAfter(limite)) {
                long retrasoMinutos = ChronoUnit.MINUTES.between(limite, ahora);
                
                // Solo enviar si es mayor a 5 minutos para evitar spam instantǭneo
                if (retrasoMinutos >= 5) {
                    String mensaje = String.format("El monitoreo de %s (Jaula %s) lleva %d minutos de retraso.",
                            h.getPaciente().getNombre(),
                            h.getJaula().getNumero(),
                            retrasoMinutos);

                    // Notificar a todos los suscritos al topico de alertas
                    messagingTemplate.convertAndSend("/topic/alertas-hospitalizacion", mensaje);
                }
            }
        }
    }
}
