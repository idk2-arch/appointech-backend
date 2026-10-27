package com.appointech.appointech_backend.domain.exception;

public class UbicacionBaseNoConfiguradaException extends RuntimeException {
    public UbicacionBaseNoConfiguradaException() {
        super("Tu ubicación base no está configurada; pídele al administrador que la cargue");
    }
}
