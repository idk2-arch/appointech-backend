package com.appointech.appointech_backend.domain.exception;

public class ReglasFranjasInvalidasException extends RuntimeException {
    public ReglasFranjasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
