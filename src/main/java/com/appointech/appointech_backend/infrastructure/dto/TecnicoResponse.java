package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;

import java.util.List;

public record TecnicoResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String direccionBase,
        Integer radioCoberturaKm,
        List<EspecialidadResponse> especialidades,
        Long usuarioId,
        String telefono,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        boolean activo,
        Double latitudBase,
        Double longitudBase,
        String fotoUrl
) {
}
