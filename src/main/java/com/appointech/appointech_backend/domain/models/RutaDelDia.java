package com.appointech.appointech_backend.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RutaDelDia(
        LocalDate fecha,
        LocalDateTime calculadaEn,
        Coordenadas origen,
        List<ParadaRuta> paradas,
        double totalKm,
        double totalMinutosDeViaje
) {
}
