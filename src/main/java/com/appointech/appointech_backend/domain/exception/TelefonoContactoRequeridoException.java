package com.appointech.appointech_backend.domain.exception;

public class TelefonoContactoRequeridoException extends RuntimeException {
    public TelefonoContactoRequeridoException() {
        super("El teléfono de contacto es obligatorio porque tu cuenta no tiene un teléfono registrado");
    }
}
