package com.appointech.appointech_backend.domain.exception;

/** La franja de trabajo no existe o no es de ese técnico (se responde 404). */
public class FranjaTrabajoNoEncontradaException extends RuntimeException {
    public FranjaTrabajoNoEncontradaException(Long franjaId) {
        super("La franja de trabajo " + franjaId + " no existe para ese técnico");
    }
}
