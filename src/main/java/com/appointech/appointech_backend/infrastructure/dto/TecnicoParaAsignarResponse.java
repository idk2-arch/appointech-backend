package com.appointech.appointech_backend.infrastructure.dto;

/** Técnico disponible para una solicitud manual; {@code distanciaKm} es null si no tiene ubicación registrada. */
public record TecnicoParaAsignarResponse(
        Long tecnicoId,
        String nombre,
        String apellido,
        String telefono,
        String fotoUrl,
        Double distanciaKm
) {
}
