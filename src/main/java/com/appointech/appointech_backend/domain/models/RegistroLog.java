package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

public class RegistroLog {

    private final Long id;
    private final String nivel;
    private final String tipo;
    private final String origen;
    private final String usuario;
    private final String mensaje;
    private final LocalDateTime creadoEn;

    public RegistroLog(Long id, String nivel, String tipo, String origen, String usuario,
                       String mensaje, LocalDateTime creadoEn) {
        this.id = id;
        this.nivel = nivel;
        this.tipo = tipo;
        this.origen = origen;
        this.usuario = usuario;
        this.mensaje = mensaje;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public String getNivel() {
        return nivel;
    }

    public String getTipo() {
        return tipo;
    }

    public String getOrigen() {
        return origen;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
