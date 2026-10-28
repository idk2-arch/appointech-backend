package com.appointech.appointech_backend.domain.exception;

/** La cita ya tiene el máximo de fotos del electrodoméstico (se responde 409). */
public class LimiteFotosCitaException extends RuntimeException {
    public LimiteFotosCitaException(int maximo) {
        super("La cita ya tiene el máximo de " + maximo + " fotos");
    }
}
