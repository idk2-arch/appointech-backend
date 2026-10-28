package com.appointech.appointech_backend.domain.exception;

/** No existe una foto con esa clave (se responde 404). */
public class FotoNoEncontradaException extends RuntimeException {
    public FotoNoEncontradaException() {
        super("No se encontró la foto");
    }
}
