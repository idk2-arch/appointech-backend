package com.appointech.appointech_backend.infrastructure.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String rol
) {
}