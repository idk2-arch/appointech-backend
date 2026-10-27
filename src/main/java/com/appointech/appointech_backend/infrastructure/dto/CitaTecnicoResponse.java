package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record CitaTecnicoResponse(
        Long id,
        String estado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String descripcionProblema,
        String direccionServicio,
        Double latitud,
        Double longitud,
        ClienteResumenResponse cliente
) {
}
