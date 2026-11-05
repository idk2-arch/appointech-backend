package com.appointech.appointech_backend.domain.models.exception;

public class DatosPagoInvalidosException extends RuntimeException {
    public DatosPagoInvalidosException(String mensaje) {
        super(mensaje);
    }
}
