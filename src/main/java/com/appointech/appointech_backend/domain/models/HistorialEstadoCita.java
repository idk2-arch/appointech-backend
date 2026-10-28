package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/**
 * Una fila del historial de la cita. {@code estadoAnterior} es null cuando no hay estado previo;
 * en una reprogramación el estado anterior y el nuevo coinciden y el motivo describe el cambio de horario.
 */
public record HistorialEstadoCita(
        Long id,
        Long citaId,
        EstadoCita estadoAnterior,
        EstadoCita estadoNuevo,
        LocalDateTime fecha,
        String actor,
        String motivo
) {
}
