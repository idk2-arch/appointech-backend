package com.appointech.appointech_backend.infrastructure.controllers;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Salud del proceso; no expone configuracion ni depende de servicios externos. */
@RestController
public class SaludController {
    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "UP");
    }
}
