package com.appointech.appointech_backend.domain.models;

import java.util.List;

/**
 * Modelo de lectura: la cita con su técnico/especialidad, su diagnóstico (dentro de la cita), su historial de estados,
 * sus fotos y, cuando se consulta para el administrador, la calificación del cliente (null si no hay o no se carga).
 */
public record DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial, List<CitaFoto> fotos,
                                  Calificacion calificacion) {

    /** Detalle sin calificación. */
    public DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial, List<CitaFoto> fotos) {
        this(detalle, historial, fotos, null);
    }

    /** Detalle sin fotos del electrodoméstico ni calificación. */
    public DetalleCitaCompleto(DetalleCita detalle, List<HistorialEstadoCita> historial) {
        this(detalle, historial, List.of(), null);
    }
}
