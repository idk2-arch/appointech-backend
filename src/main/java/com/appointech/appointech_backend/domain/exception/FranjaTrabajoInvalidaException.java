package com.appointech.appointech_backend.domain.exception;

/** Los datos de la franja de trabajo no son válidos (se responde 400). */
public class FranjaTrabajoInvalidaException extends RuntimeException {
    public FranjaTrabajoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
