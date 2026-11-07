package com.appointech.appointech_backend.infrastructure.dto;

/** Fecha en formato yyyy-MM-dd y horas en formato HH:mm (se validan al convertirlas, con un 400 claro). */
public record FranjaTrabajoRequest(String fecha, String horaInicio, String horaFin) {
}
