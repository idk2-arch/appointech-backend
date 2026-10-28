package com.appointech.appointech_backend.domain.exception;

/** El almacenamiento de objetos no respondió (se responde 503). */
public class AlmacenamientoNoDisponibleException extends RuntimeException {
    public AlmacenamientoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
