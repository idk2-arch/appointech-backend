package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

/** Calificación recibida por un técnico; del cliente solo se muestra el nombre de pila. */
public record CalificacionRecibidaResponse(Long citaId, int puntuacion, String comentario, LocalDateTime fecha,
                                           String clienteNombre) {
}
