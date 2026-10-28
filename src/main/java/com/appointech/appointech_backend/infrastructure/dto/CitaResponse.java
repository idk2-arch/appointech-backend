package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        Long tecnicoId,
        Long especialidadId,
        String descripcionProblema,
        String direccionServicio,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String estado,
        String tecnicoNombre,
        String tecnicoApellido,
        String tecnicoTelefono,
        String especialidadNombre,
        String tecnicoFotoUrl
) {
}
