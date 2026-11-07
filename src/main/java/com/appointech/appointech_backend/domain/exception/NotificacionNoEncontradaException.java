package com.appointech.appointech_backend.domain.exception;

/** La notificación no existe o es de otro usuario (en ambos casos se responde 404). */
public class NotificacionNoEncontradaException extends RuntimeException {
    public NotificacionNoEncontradaException(Long id) {
        super("La notificación " + id + " no existe");
    }
}
