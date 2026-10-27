package com.appointech.appointech_backend.domain.exception;

public class BloqueNoEncontradoException extends RuntimeException {
    public BloqueNoEncontradoException(Long id) {
        super("No se encontró el bloque con id: " + id);
    }
}
