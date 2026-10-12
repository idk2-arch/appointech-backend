package com.appointech.appointech_backend.domain.exception;

public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(Long id) {
        super("No se encontró la solicitud con id: " + id);
    }
}
