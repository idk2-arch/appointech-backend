package com.appointech.appointech_backend.domain.models;

import java.util.List;

/** Modelo de lectura: la cita con su técnico/especialidad, su diagnóstico (dentro de la cita), su historial de estados y sus fotos. */
public record DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial, List<CitaFoto> fotos) {

    /** Detalle sin fotos del electrodoméstico. */
    public DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial) {
        this(detalle, historial, List.of());
    }
}
