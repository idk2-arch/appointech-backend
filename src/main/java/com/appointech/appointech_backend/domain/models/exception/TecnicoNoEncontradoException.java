package com.appointech.appointech_backend.domain.models.exception;

public class TecnicoNoEncontradoException extends RuntimeException {
    public TecnicoNoEncontradoException(Long id) {
        super("No se encontró el técnico con id: " + id);
    }
}