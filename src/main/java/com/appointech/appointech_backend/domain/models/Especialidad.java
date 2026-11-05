package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;

public class Especialidad {
    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private BigDecimal costoReferencia;

    public Especialidad() {
    }

    public Especialidad(Long id, String nombre, String descripcion, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getCostoReferencia() {
        return costoReferencia;
    }

    public void setCostoReferencia(BigDecimal costoReferencia) {
        this.costoReferencia = costoReferencia;
    }
}