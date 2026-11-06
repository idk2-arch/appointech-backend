package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Rango de fechas de las métricas: ambos extremos son inclusivos y el rango no supera {@value #DIAS_MAXIMOS} días. */
public record RangoMetricas(LocalDate desde, LocalDate hasta) {

    public static final int DIAS_POR_DEFECTO = 30;
    public static final int DIAS_MAXIMOS = 366;

    /**
     * Completa lo que falte y valida. Sin {@code hasta} se usa {@code hoy}; sin {@code desde}, los {@value #DIAS_POR_DEFECTO}
     * días que terminan en {@code hasta} (hasta incluido).
     *
     * @throws FiltroCitasInvalidoException si desde es posterior a hasta o el rango supera {@value #DIAS_MAXIMOS} días
     */
    public static RangoMetricas de(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : desde;
        if (inicio.isAfter(fin)) {
            throw new FiltroCitasInvalidoException("desde no puede ser posterior a hasta");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > DIAS_MAXIMOS) {
            throw new FiltroCitasInvalidoException("El rango no puede superar " + DIAS_MAXIMOS + " días");
        }
        return new RangoMetricas(inicio, fin);
    }

    /** Cantidad de días del rango, contando ambos extremos. */
    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }
}
