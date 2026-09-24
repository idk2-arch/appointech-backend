package com.appointech.appointech_backend.infrastructure.dto;

public record EspecialidadAdminResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        long tecnicosAsignados
) {
}