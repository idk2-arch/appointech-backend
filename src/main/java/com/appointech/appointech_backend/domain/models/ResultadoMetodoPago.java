package com.appointech.appointech_backend.domain.models;

/** El pago tras elegir el método; urlPago solo viene en los pagos en línea. */
public record ResultadoMetodoPago(Pago pago, String urlPago) {
}
