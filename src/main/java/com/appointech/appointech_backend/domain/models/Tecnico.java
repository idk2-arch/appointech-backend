package com.appointech.appointech_backend.domain.models;

import java.util.List;

public class Tecnico {
    private Long id;
    private Usuario usuario;
    private String direccionBase;
    private Double latitudBase;
    private Double longitudBase;
    private Integer radioCoberturaKm;
    private List<Especialidad> especialidades;

    public Tecnico() {
    }

    public Tecnico(Long id, Usuario usuario, String direccionBase, Double latitudBase,
                   Double longitudBase, Integer radioCoberturaKm, List<Especialidad> especialidades) {
        this.id = id;
        this.usuario = usuario;
        this.direccionBase = direccionBase;
        this.latitudBase = latitudBase;
        this.longitudBase = longitudBase;
        this.radioCoberturaKm = radioCoberturaKm;
        this.especialidades = especialidades;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDireccionBase() {
        return direccionBase;
    }

    public void setDireccionBase(String direccionBase) {
        this.direccionBase = direccionBase;
    }

    public Double getLatitudBase() {
        return latitudBase;
    }

    public void setLatitudBase(Double latitudBase) {
        this.latitudBase = latitudBase;
    }

    public Double getLongitudBase() {
        return longitudBase;
    }

    public void setLongitudBase(Double longitudBase) {
        this.longitudBase = longitudBase;
    }

    public Integer getRadioCoberturaKm() {
        return radioCoberturaKm;
    }

    public void setRadioCoberturaKm(Integer radioCoberturaKm) {
        this.radioCoberturaKm = radioCoberturaKm;
    }

    public List<Especialidad> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(List<Especialidad> especialidades) {
        this.especialidades = especialidades;
    }
}