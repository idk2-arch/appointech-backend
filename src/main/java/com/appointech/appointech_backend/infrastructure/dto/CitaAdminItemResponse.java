package com.appointech.appointech_backend.infrastructure.dto;

/** Fila del listado de citas del administrador: fecha yyyy-MM-dd, horas HH:mm. */
public record CitaAdminItemResponse(
        Long id,
        String fecha,
        String horaInicio,
        String horaFin,
        String estado,
        String nombreCliente,
        String nombreTecnico,
        String servicio,
        String direccion
) {
}
