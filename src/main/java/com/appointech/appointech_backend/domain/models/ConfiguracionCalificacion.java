package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Parámetros de las calificaciones: días, desde que la cita se finaliza, durante los que el cliente puede calificar. */
public record ConfiguracionCalificacion(int plazoDias) {

    public ConfiguracionCalificacion {
        if (plazoDias < 1) {
            throw new IllegalArgumentException("El plazo para calificar debe ser de al menos 1 día");
        }
    }

    /** Último instante en que se puede calificar una cita finalizada en {@code finalizadaEn} (el límite exacto aún vale). */
    public LocalDateTime limite(LocalDateTime finalizadaEn) {
        return finalizadaEn.plusDays(plazoDias);
    }
}
