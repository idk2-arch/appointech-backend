package com.appointech.appointech_backend.domain.exception;

public class TelefonoWhatsappInvalidoException extends RuntimeException {
    public TelefonoWhatsappInvalidoException(String mensaje) {
        super(mensaje);
    }
}
