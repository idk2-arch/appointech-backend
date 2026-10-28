package com.appointech.appointech_backend.domain.models;

import java.util.List;

/** Modelo de lectura: la cita con su técnico/especialidad, su diagnóstico (dentro de la cita) y su historial de estados. */
public record DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial) {
}
