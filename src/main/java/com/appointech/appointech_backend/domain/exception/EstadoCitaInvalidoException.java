package com.appointech.appointech_backend.domain.exception;

/** La operación no es válida para el estado actual de la cita (se responde 409). */
public class EstadoCitaInvalidoException extends RuntimeException {
    public EstadoCitaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
