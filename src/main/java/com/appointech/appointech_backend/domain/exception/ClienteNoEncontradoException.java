package com.appointech.appointech_backend.domain.exception;

public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String correo) {
        super("No se encontró un cliente asociado a la cuenta: " + correo);
    }
}
