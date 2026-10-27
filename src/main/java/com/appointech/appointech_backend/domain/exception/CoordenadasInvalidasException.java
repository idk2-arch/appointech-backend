package com.appointech.appointech_backend.domain.exception;

public class CoordenadasInvalidasException extends RuntimeException {
    public CoordenadasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
