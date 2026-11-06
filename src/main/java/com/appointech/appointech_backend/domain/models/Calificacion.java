package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.CalificacionInvalidaException;

import java.time.LocalDateTime;

/**
 * Calificación que el cliente da a una cita finalizada. Es inmutable: no se edita ni se borra.
 * El comentario es opcional; vacío o en blanco se guarda como null.
 */
public record Calificacion(Long id, Long citaId, int puntuacion, String comentario, LocalDateTime creadaEn) {

    public static final int PUNTUACION_MINIMA = 1;
    public static final int PUNTUACION_MAXIMA = 5;
    public static final int LONGITUD_MAXIMA_COMENTARIO = 500;

    /** Calificación nueva (sin id) con sus datos validados. */
    public static Calificacion nueva(Long citaId, Integer puntuacion, String comentario, LocalDateTime creadaEn) {
        if (puntuacion == null || puntuacion < PUNTUACION_MINIMA || puntuacion > PUNTUACION_MAXIMA) {
            throw new CalificacionInvalidaException(
                    "La puntuación debe ser un número entero entre " + PUNTUACION_MINIMA + " y " + PUNTUACION_MAXIMA);
        }
        String texto = comentario == null || comentario.isBlank() ? null : comentario.strip();
        if (texto != null && texto.length() > LONGITUD_MAXIMA_COMENTARIO) {
            throw new CalificacionInvalidaException(
                    "El comentario no puede superar los " + LONGITUD_MAXIMA_COMENTARIO + " caracteres");
        }
        return new Calificacion(null, citaId, puntuacion, texto, creadaEn);
    }
}
