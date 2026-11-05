package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;

/**
 * Lo que la pasarela necesita para cobrar. El dinero viaja en centavos enteros: es el único punto donde se
 * abandona BigDecimal.
 *
 * @param referencia único por intento de pago.
 */
public record SolicitudPago(String referencia, long montoEnCentavos, String moneda, String correoCliente,
                            String urlRetorno) {

    public static final String MONEDA_COP = "COP";

    public static SolicitudPago cobro(String referencia, BigDecimal monto, String correoCliente, String urlRetorno) {
        return new SolicitudPago(referencia, Dinero.aCentavos(monto), MONEDA_COP, correoCliente, urlRetorno);
    }
}
