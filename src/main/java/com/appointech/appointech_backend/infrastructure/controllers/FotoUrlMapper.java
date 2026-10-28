package com.appointech.appointech_backend.infrastructure.controllers;

/** Construye la URL pública con la que el frontend pide una foto de perfil al backend. */
final class FotoUrlMapper {

    static final String RUTA_FOTOS = "/api/fotos/";

    private FotoUrlMapper() {
    }

    /** Null cuando el usuario no tiene foto. */
    static String aUrl(String fotoClave) {
        return fotoClave == null ? null : RUTA_FOTOS + fotoClave;
    }
}
