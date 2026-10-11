package com.appointech.appointech_backend.domain.models;

import java.time.LocalTime;

public class ConfiguracionFranjas {

    private Long id;
    private Integer duracionBloqueMinutos;
    private Integer radioMaximoKm;
    private LocalTime horaInicioJornada;
    private LocalTime horaFinJornada;

    public ConfiguracionFranjas() {
    }

    public ConfiguracionFranjas(Long id, Integer duracionBloqueMinutos, Integer radioMaximoKm,
                                LocalTime horaInicioJornada, LocalTime horaFinJornada) {
        this.id = id;
        this.duracionBloqueMinutos = duracionBloqueMinutos;
        this.radioMaximoKm = radioMaximoKm;
        this.horaInicioJornada = horaInicioJornada;
        this.horaFinJornada = horaFinJornada;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDuracionBloqueMinutos() {
        return duracionBloqueMinutos;
    }

    public void setDuracionBloqueMinutos(Integer duracionBloqueMinutos) {
        this.duracionBloqueMinutos = duracionBloqueMinutos;
    }

    public Integer getRadioMaximoKm() {
        return radioMaximoKm;
    }

    public void setRadioMaximoKm(Integer radioMaximoKm) {
        this.radioMaximoKm = radioMaximoKm;
    }

    public LocalTime getHoraInicioJornada() {
        return horaInicioJornada;
    }

    public void setHoraInicioJornada(LocalTime horaInicioJornada) {
        this.horaInicioJornada = horaInicioJornada;
    }

    public LocalTime getHoraFinJornada() {
        return horaFinJornada;
    }

    public void setHoraFinJornada(LocalTime horaFinJornada) {
        this.horaFinJornada = horaFinJornada;
    }
}
