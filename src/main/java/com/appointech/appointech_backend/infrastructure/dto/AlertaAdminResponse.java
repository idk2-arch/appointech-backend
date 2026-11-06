package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

/** Una alerta para el administrador; {@code solicitudId} es la solicitud pendiente que la originó (visible en RF18). */
public record AlertaAdminResponse(
        String tipo,
        String mensaje,
        Long solicitudId,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        String telefono,
        Long especialidadId,
        String especialidadNombre,
        String direccionServicio,
        LocalDateTime creadaEn
) {
}
