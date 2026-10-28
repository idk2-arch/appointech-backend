package com.appointech.appointech_backend.domain.exception;

/** Los filtros o la paginación de la consulta de citas no son válidos (se responde 400). */
public class FiltroCitasInvalidoException extends RuntimeException {
    public FiltroCitasInvalidoException(String mensaje) {
        super(mensaje);
    }
}
