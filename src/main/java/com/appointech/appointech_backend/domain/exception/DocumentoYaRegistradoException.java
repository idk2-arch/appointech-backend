package com.appointech.appointech_backend.domain.exception;

public class DocumentoYaRegistradoException extends RuntimeException {
    public DocumentoYaRegistradoException(String numeroDocumento) {
        super("El número de documento ya está registrado: " + numeroDocumento);
    }
}
