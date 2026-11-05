package com.appointech.appointech_backend.domain.models.exception;

public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException(Long citaId) {
        super("La cita " + citaId + " aún no tiene un pago registrado");
    }
}
