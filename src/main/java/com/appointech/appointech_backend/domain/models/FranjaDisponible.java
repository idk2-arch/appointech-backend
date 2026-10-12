package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

public class FranjaDisponible {

    private final Long tecnicoId;
    private final LocalDateTime inicio;
    private final LocalDateTime fin;
    private final double distanciaKm;

    public FranjaDisponible(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin, double distanciaKm) {
        this.tecnicoId = tecnicoId;
        this.inicio = inicio;
        this.fin = fin;
        this.distanciaKm = distanciaKm;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }
}
