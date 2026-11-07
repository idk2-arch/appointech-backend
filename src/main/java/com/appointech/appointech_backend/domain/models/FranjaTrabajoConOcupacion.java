package com.appointech.appointech_backend.domain.models;

/** Una franja de trabajo con cuántos bloques tiene y cuántos están ocupados por citas. */
public record FranjaTrabajoConOcupacion(FranjaTrabajo franja, int bloques, int bloquesOcupados) {
}
