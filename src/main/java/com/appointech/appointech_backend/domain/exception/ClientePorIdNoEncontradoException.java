package com.appointech.appointech_backend.domain.exception;

public class ClientePorIdNoEncontradoException extends RuntimeException {
    public ClientePorIdNoEncontradoException(Long id) {
        super("No se encontró el cliente con id: " + id);
    }
}
