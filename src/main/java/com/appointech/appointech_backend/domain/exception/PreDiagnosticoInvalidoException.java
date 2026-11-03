package com.appointech.appointech_backend.domain.exception;

/** La descripción o la especialidad del pre-diagnóstico no son válidas (se responde 400). */
public class PreDiagnosticoInvalidoException extends RuntimeException {
    public PreDiagnosticoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
