package com.appointech.appointech_backend.domain.exception;

public class FranjaNoDisponibleException extends RuntimeException {
    public FranjaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
