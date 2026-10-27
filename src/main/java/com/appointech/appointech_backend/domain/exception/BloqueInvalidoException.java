package com.appointech.appointech_backend.domain.exception;

public class BloqueInvalidoException extends RuntimeException {
    public BloqueInvalidoException(String mensaje) {
        super(mensaje);
    }
}
