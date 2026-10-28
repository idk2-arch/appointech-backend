package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una cita para el cliente dueño y el administrador; {@code diagnostico} es null mientras no exista. */
public record CitaDetalleResponse(
        Long id,
        String estado,
        Long tecnicoId,
        String tecnicoNombre,
        String tecnicoApellido,
        String tecnicoTelefono,
        String especialidadNombre,
        String descripcionProblema,
        String direccionServicio,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        DiagnosticoResponse diagnostico,
        List<HistorialEstadoResponse> historial,
        String fotoUrl
) {
}
