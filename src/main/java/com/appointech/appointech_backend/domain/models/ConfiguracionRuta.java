package com.appointech.appointech_backend.domain.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reglas de puntualidad de la ruta del técnico.
 *
 * @param margenMinutos              minutos que se suman al viaje entre citas al decidir si el técnico llega.
 * @param toleranciaRetrasoMinutos   retraso (en minutos enteros) que aún se considera llegar a tiempo.
 */
public record ConfiguracionRuta(int margenMinutos, int toleranciaRetrasoMinutos) {

    /** Zona en la que se interpretan las horas locales de las citas (Colombia no usa horario de verano). */
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public ConfiguracionRuta {
        if (toleranciaRetrasoMinutos < 0) {
            throw new IllegalArgumentException("La tolerancia de retraso no puede ser negativa");
        }
    }

    /** Sin tolerancia: la regla estricta (cualquier retraso es llegar tarde). */
    public ConfiguracionRuta(int margenMinutos) {
        this(margenMinutos, 0);
    }

    /** Minutos enteros de retraso (redondeados hacia arriba) al llegar a {@code llegada} para una cita que empieza en {@code inicioCita}; 0 si llega a tiempo. */
    public int retrasoEstimadoMinutos(LocalDateTime llegada, LocalDateTime inicioCita) {
        long segundos = Duration.between(inicioCita.atZone(ZONA), llegada.plusMinutes(margenMinutos).atZone(ZONA)).getSeconds();
        return segundos <= 0 ? 0 : (int) ((segundos + 59) / 60);
    }

    public boolean alcanzaATiempo(int retrasoEstimadoMinutos) {
        return retrasoEstimadoMinutos <= toleranciaRetrasoMinutos;
    }
}
