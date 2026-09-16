package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record UsuarioAdminResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        String tipoDocumento,
        String numeroDocumento,
        String rol,
        boolean activo,
        LocalDateTime creadoEn
) {
}