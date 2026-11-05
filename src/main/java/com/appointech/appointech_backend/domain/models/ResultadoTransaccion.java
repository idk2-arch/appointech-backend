package com.appointech.appointech_backend.domain.models;

/** Estado de una transacción consultado directamente a la pasarela (nunca lo que diga el cliente). */
public record ResultadoTransaccion(String idTransaccion, String referencia, long montoEnCentavos,
                                   EstadoTransaccionPasarela estado) {
}
