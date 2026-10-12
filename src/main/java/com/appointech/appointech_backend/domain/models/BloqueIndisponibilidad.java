package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

public class BloqueIndisponibilidad {

    private Long id;
    private Long tecnicoId;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private String motivo;

    public BloqueIndisponibilidad() {
    }

    public BloqueIndisponibilidad(Long id, Long tecnicoId, LocalDateTime inicio, LocalDateTime fin, String motivo) {
        this.id = id;
        this.tecnicoId = tecnicoId;
        this.inicio = inicio;
        this.fin = fin;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
