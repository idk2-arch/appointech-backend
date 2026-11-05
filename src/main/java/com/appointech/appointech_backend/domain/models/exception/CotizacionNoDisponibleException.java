package com.appointech.appointech_backend.domain.models.exception;

public class CotizacionNoDisponibleException extends RuntimeException {
    public CotizacionNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
