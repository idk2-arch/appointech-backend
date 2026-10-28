package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;

public record ClienteAdminResponse(
        Long id,
        Long usuarioId,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        boolean activo,
        String direccion,
        Double latitud,
        Double longitud,
        String fotoUrl
) {
}
