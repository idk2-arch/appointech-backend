package com.appointech.appointech_backend.domain.models.exception;

public class PagoNoPermitidoException extends RuntimeException {
    public PagoNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
