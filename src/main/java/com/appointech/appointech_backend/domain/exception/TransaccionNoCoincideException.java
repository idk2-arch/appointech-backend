package com.appointech.appointech_backend.domain.exception;

/** La transacción consultada no corresponde a la referencia o al monto del pago (se responde 422). */
public class TransaccionNoCoincideException extends RuntimeException {
    public TransaccionNoCoincideException() {
        super("La transacción no corresponde a este pago: la referencia o el monto no coinciden");
    }
}
