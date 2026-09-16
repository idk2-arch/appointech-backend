package com.appointech.appointech_backend.infrastructure.dto;

import java.util.List;

public record TecnicoResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String direccionBase,
        Integer radioCoberturaKm,
        List<EspecialidadResponse> especialidades
) {
}