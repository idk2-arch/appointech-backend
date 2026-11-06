package com.appointech.appointech_backend.domain.models;

/**
 * Técnico que puede atender una solicitud manual en un horario dado. {@code distanciaKm} es la distancia desde la base
 * del técnico hasta la dirección del cliente, o null si no se puede calcular (sin ubicación registrada).
 */
public record TecnicoDisponible(
        Long tecnicoId,
        String nombre,
        String apellido,
        String telefono,
        String fotoClave,
        Double distanciaKm
) {
}
