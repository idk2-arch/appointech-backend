package com.appointech.appointech_backend.domain.exception;

/** La pasarela de pagos falló o no respondió; el pago en efectivo sigue disponible (se responde 503). */
public class PasarelaNoDisponibleException extends RuntimeException {
    public PasarelaNoDisponibleException() {
        super("El pago en línea no está disponible por ahora; intenta más tarde o paga en efectivo");
    }
}
