package com.appointech.appointech_backend.domain.models;

public class Cliente {
    private Long id;
    private Usuario usuario;
    private String direccion;
    private Double latitud;
    private Double longitud;

    public Cliente() {
    }

    public Cliente(Long id, Usuario usuario, String direccion, Double latitud, Double longitud) {
        this.id = id;
        this.usuario = usuario;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
}