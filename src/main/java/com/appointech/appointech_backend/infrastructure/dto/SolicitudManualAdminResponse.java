package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record SolicitudManualAdminResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        String telefono,
        String direccionServicio,
        String descripcionProblema,
        Long especialidadId,
        String especialidadNombre,
        Double latitud,
        Double longitud,
        LocalDateTime creadaEn
) {
}
