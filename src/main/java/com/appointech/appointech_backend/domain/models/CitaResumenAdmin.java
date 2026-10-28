package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Modelo de lectura: una fila del listado de citas del administrador; nombreTecnico es null si la cita no tiene técnico. */
public record CitaResumenAdmin(
        Long id,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoCita estado,
        String nombreCliente,
        String nombreTecnico,
        String servicio,
        String direccion
) {
}
