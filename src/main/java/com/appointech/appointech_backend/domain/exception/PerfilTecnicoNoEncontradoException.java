package com.appointech.appointech_backend.domain.exception;

public class PerfilTecnicoNoEncontradoException extends RuntimeException {
    public PerfilTecnicoNoEncontradoException(String correo) {
        super("No existe un técnico asociado al usuario: " + correo);
    }
}
