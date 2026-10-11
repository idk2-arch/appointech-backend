package com.appointech.appointech_backend.domain.exception;

public class FranjaInvalidaException extends RuntimeException {
    public FranjaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
