package com.appointech.appointech_backend.domain.models;

/** Formatos de imagen aceptados para la foto de perfil (SVG y cualquier otro quedan fuera). */
public enum TipoImagen {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    private final String tipoContenido;

    TipoImagen(String tipoContenido) {
        this.tipoContenido = tipoContenido;
    }

    public String tipoContenido() {
        return tipoContenido;
    }
}
