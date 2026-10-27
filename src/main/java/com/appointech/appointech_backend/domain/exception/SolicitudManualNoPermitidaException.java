package com.appointech.appointech_backend.domain.exception;

public class SolicitudManualNoPermitidaException extends RuntimeException {
    public SolicitudManualNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
