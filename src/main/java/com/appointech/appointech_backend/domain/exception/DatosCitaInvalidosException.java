package com.appointech.appointech_backend.domain.exception;

/** Datos de entrada de una operación sobre la cita que no cumplen las reglas (motivo, diagnóstico); se responde 400. */
public class DatosCitaInvalidosException extends RuntimeException {
    public DatosCitaInvalidosException(String mensaje) {
        super(mensaje);
    }
}
