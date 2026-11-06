package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

/** Quién y cuándo asignó manualmente la cita; los datos son null en las asignaciones anteriores a la trazabilidad. */
public record AsignacionManualResponse(
        Long asignadaPorId,
        String asignadaPorNombre,
        LocalDateTime asignadaEn
) {
}
