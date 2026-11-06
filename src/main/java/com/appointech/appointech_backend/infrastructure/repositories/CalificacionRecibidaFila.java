package com.appointech.appointech_backend.infrastructure.repositories;

import java.time.LocalDateTime;

/** Proyección de una calificación recibida por un técnico, con el nombre de pila del cliente (una sola consulta con joins). */
public record CalificacionRecibidaFila(
        Long citaId,
        Short puntuacion,
        String comentario,
        LocalDateTime creadaEn,
        String clienteNombre
) {
}
