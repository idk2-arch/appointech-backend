package com.appointech.appointech_backend.domain.exception;

/** La operación no es válida para el estado actual del pago (se responde 409). */
public class PagoInvalidoException extends RuntimeException {
    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
