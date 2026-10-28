package com.appointech.notifications.infrastructure.controllers;

import com.appointech.notifications.application.EmailNotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** HTTP routes and JSON field names are the contract with the main backend and stay in Spanish. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final EmailNotificationService emailNotificationService;

    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/salud")
    public Map<String, String> health() {
        return Map.of("servicio", "microservicio-notificaciones", "estado", "UP");
    }

    /** Status changes, cancellations and rescheduling: for now they are only logged. */
    @PostMapping("/cita-actualizada")
    public ResponseEntity<Map<String, Object>> appointmentUpdated(@RequestBody Map<String, Object> event,
                                                                  Authentication authentication) {
        log.info("Notificación de cambio de cita {} (estado {}) recibida de {}",
                event.get("citaId"), event.get("estado"), authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("recibido", true, "citaId", event.getOrDefault("citaId", "")));
    }

    @PostMapping("/cita-confirmada")
    public ResponseEntity<Map<String, Object>> appointmentConfirmed(@Valid @RequestBody AppointmentConfirmedRequest request,
                                                                    Authentication authentication) {
        log.info("Notificación de cita confirmada {} recibida de {}", request.appointmentId(), authentication.getName());
        emailNotificationService.notifyAppointmentConfirmed(request.toEvent());
        return accepted(request.appointmentId());
    }

    @PostMapping("/cita-finalizada")
    public ResponseEntity<Map<String, Object>> appointmentCompleted(@Valid @RequestBody AppointmentCompletedRequest request,
                                                                    Authentication authentication) {
        log.info("Notificación de cita finalizada {} recibida de {}", request.appointmentId(), authentication.getName());
        emailNotificationService.notifyAppointmentCompleted(request.toEvent());
        return accepted(request.appointmentId());
    }

    private ResponseEntity<Map<String, Object>> accepted(Long appointmentId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("recibido", true, "citaId", appointmentId == null ? "" : appointmentId));
    }
}
