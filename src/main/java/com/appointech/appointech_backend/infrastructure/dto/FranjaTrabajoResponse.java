package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

/** Una franja de trabajo; {@code bloques} son las citas que caben y {@code bloquesOcupados} las que ya tienen cita. */
public record FranjaTrabajoResponse(Long id, Long tecnicoId, String fecha, String horaInicio, String horaFin,
                                    int bloques, int bloquesOcupados, LocalDateTime creadaEn) {
}
