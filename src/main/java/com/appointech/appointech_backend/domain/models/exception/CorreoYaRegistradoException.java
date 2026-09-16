package com.appointech.appointech_backend.domain.models.exception;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String correo) {
        super("El correo ya está registrado: " + correo);
    }
}