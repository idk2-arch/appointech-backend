package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;

/**
 * Punto geográfico validado. Además del rango mundial, exige que caiga en Colombia
 * para detectar fácilmente latitud y longitud invertidas.
 */
public record Coordenadas(double latitud, double longitud) {

    private static final double LATITUD_MINIMA_COLOMBIA = -5.0;
    private static final double LATITUD_MAXIMA_COLOMBIA = 14.0;
    private static final double LONGITUD_MINIMA_COLOMBIA = -82.0;
    private static final double LONGITUD_MAXIMA_COLOMBIA = -66.0;

    public Coordenadas {
        if (Double.isNaN(latitud) || latitud < -90 || latitud > 90) {
            throw new CoordenadasInvalidasException("La latitud debe estar entre -90 y 90");
        }
        if (Double.isNaN(longitud) || longitud < -180 || longitud > 180) {
            throw new CoordenadasInvalidasException("La longitud debe estar entre -180 y 180");
        }
        if (latitud < LATITUD_MINIMA_COLOMBIA || latitud > LATITUD_MAXIMA_COLOMBIA
                || longitud < LONGITUD_MINIMA_COLOMBIA || longitud > LONGITUD_MAXIMA_COLOMBIA) {
            throw new CoordenadasInvalidasException(
                    "Las coordenadas deben estar dentro de Colombia (latitud entre -5 y 14, longitud entre -82 y -66). "
                            + "Verifica que la latitud y la longitud no estén invertidas");
        }
    }

    /** Ambos valores obligatorios. */
    public static Coordenadas de(Double latitud, Double longitud) {
        if (latitud == null || longitud == null) {
            throw new CoordenadasInvalidasException("La latitud y la longitud son obligatorias");
        }
        return new Coordenadas(latitud, longitud);
    }

    /** Ambos valores opcionales: si llega uno debe llegar el otro; si no llega ninguno devuelve null. */
    public static Coordenadas opcional(Double latitud, Double longitud) {
        if (latitud == null && longitud == null) {
            return null;
        }
        return de(latitud, longitud);
    }
}
