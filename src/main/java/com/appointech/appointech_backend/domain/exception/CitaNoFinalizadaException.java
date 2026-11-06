package com.appointech.appointech_backend.domain.exception;

/** Solo se puede calificar una cita finalizada (se responde 409). */
public class CitaNoFinalizadaException extends RuntimeException {
    public CitaNoFinalizadaException(String estadoActual) {
        super("Solo puedes calificar una cita finalizada (estado actual: " + estadoActual + ")");
    }
}
