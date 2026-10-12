package com.appointech.notificaciones;

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

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private static final Logger registro = LoggerFactory.getLogger(NotificacionController.class);

    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("servicio", "microservicio-notificaciones", "estado", "UP");
    }

    @PostMapping("/cita-confirmada")
    public ResponseEntity<Map<String, Object>> citaConfirmada(@RequestBody Map<String, Object> evento,
                                                              Authentication autenticacion) {
        registro.info("Notificación de cita confirmada recibida de {}: {}", autenticacion.getName(), evento);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("recibido", true, "citaId", evento.getOrDefault("citaId", "")));
    }
}
