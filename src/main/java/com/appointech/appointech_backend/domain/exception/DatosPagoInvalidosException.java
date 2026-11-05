package com.appointech.appointech_backend.domain.exception;

/** Datos de pago inválidos, como un monto no positivo (se responde 400). */
public class DatosPagoInvalidosException extends RuntimeException {
    public DatosPagoInvalidosException(String mensaje) {
        super(mensaje);
    }
}
