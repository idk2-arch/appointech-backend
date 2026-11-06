package com.appointech.appointech_backend.domain.exception;

/** Los datos de la calificación no son válidos (se responde 400). */
public class CalificacionInvalidaException extends RuntimeException {
    public CalificacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
