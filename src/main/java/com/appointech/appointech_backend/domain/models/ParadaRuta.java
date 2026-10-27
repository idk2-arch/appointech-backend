package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

public record ParadaRuta(
        int orden,
        Long citaId,
        LocalDateTime inicio,
        LocalDateTime fin,
        String direccionServicio,
        Coordenadas ubicacion,
        ClienteResumen cliente,
        double distanciaKm,
        double minutosDesdeAnterior,
        LocalDateTime horaEstimadaLlegada,
        boolean alcanzaATiempo,
        int retrasoEstimadoMinutos
) {
}
