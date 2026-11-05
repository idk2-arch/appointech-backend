package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reglas de redondeo del dinero: pesos con dos decimales en el dominio y centavos enteros en la pasarela. */
public final class Dinero {

    private Dinero() {
    }

    public static BigDecimal redondear(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    public static long aCentavos(BigDecimal monto) {
        return redondear(monto).movePointRight(2).longValueExact();
    }
}
