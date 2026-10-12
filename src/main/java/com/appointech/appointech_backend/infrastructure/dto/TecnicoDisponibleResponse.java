package com.appointech.appointech_backend.infrastructure.dto;

public record TecnicoDisponibleResponse(
        Long id,
        String nombre,
        Integer radioCoberturaKm
) {
}
