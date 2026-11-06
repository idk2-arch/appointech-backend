package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

/** Calificación tal como la ve el administrador dentro del detalle de la cita. */
public record CalificacionAdminResponse(int puntuacion, String comentario, LocalDateTime fecha) {
}
