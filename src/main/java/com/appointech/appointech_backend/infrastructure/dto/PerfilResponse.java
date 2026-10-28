package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;

import java.util.List;

public record PerfilResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String rol,
        String direccion,
        Integer radioCoberturaKm,
        List<EspecialidadResponse> especialidades,
        String fotoUrl
) {
}
