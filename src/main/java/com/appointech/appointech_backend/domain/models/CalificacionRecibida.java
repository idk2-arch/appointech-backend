package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Calificación que recibió un técnico; solo lleva el nombre de pila del cliente (sin apellido ni datos de contacto). */
public record CalificacionRecibida(
        Long citaId,
        int puntuacion,
        String comentario,
        LocalDateTime fecha,
        String clienteNombre
) {
}
