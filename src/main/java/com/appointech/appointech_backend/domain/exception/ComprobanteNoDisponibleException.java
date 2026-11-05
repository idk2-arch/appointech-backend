package com.appointech.appointech_backend.domain.exception;

/** Todavía no existe comprobante para esta cita (se responde 409). */
public class ComprobanteNoDisponibleException extends RuntimeException {
    public ComprobanteNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
