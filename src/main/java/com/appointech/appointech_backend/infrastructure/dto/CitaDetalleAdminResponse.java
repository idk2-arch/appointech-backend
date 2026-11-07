package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de una cita para el administrador: los mismos campos que {@link CitaDetalleResponse} más, al final,
 * la calificación del cliente (null si aún no calificó) y, de último, la fecha que el cliente consultaba si pidió una
 * solicitud manual (null si no aplica).
 */
public record CitaDetalleAdminResponse(
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
        String tecnicoFotoUrl,
        List<FotoCitaResponse> fotosElectrodomestico,
        boolean asignadaManualmente,
        AsignacionManualResponse asignacionManual,
        CalificacionAdminResponse calificacion,
        LocalDate fechaDeseada
) {
}
