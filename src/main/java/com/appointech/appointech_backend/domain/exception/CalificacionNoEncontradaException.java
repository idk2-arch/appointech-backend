package com.appointech.appointech_backend.domain.exception;

/** La cita aún no tiene calificación (se responde 404). */
public class CalificacionNoEncontradaException extends RuntimeException {
    public CalificacionNoEncontradaException(Long citaId) {
        super("La cita " + citaId + " aún no tiene calificación");
    }
}
