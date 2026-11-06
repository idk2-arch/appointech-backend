package com.appointech.appointech_backend.domain.exception;

/** Pasó el plazo para calificar la cita (se responde 409). */
public class PlazoCalificacionVencidoException extends RuntimeException {
    public PlazoCalificacionVencidoException(int plazoDias) {
        super("El plazo de " + plazoDias + " días para calificar esta cita ya venció");
    }
}
