package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;

import java.time.LocalDateTime;

/** Proyección de una fila del listado de citas del administrador (una sola consulta con joins). */
public record CitaAdminFila(
        Long id,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoCitaEntity estado,
        String clienteNombre,
        String clienteApellido,
        String tecnicoNombre,
        String tecnicoApellido,
        String servicio,
        String direccion,
        String tecnicoFotoClave
) {
}
