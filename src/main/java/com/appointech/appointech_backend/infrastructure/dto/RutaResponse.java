package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RutaResponse(
        LocalDate fecha,
        LocalDateTime calculadaEn,
        Punto origen,
        List<Parada> paradas,
        double totalKm,
        double totalMinutosDeViaje
) {

    public record Punto(double latitud, double longitud) {
    }

    public record Parada(
            int orden,
            Long citaId,
            LocalDateTime inicio,
            LocalDateTime fin,
            String direccionServicio,
            double latitud,
            double longitud,
            ClienteResumenResponse cliente,
            double distanciaKm,
            double minutosDesdeAnterior,
            LocalDateTime horaEstimadaLlegada,
            boolean alcanzaATiempo,
            int retrasoEstimadoMinutos
    ) {
    }
}
