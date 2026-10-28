package com.appointech.appointech_backend.domain.exception;

/** La imagen enviada no cumple las reglas de formato o tamaño (se responde 400). */
public class ImagenInvalidaException extends RuntimeException {
    public ImagenInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ImagenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
