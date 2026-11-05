package com.appointech.appointech_backend.domain.exception;

/** La cita aún no tiene un pago registrado (se responde 404). */
public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException(Long citaId) {
        super("La cita " + citaId + " todavía no tiene un pago registrado");
    }
}
