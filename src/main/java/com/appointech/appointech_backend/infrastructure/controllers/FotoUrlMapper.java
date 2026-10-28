package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.infrastructure.dto.FotoCitaResponse;

import java.util.List;

/** Construye las URL con las que el frontend pide fotos al backend (perfil: pública; cita: con sesión). */
final class FotoUrlMapper {

    static final String RUTA_FOTOS = "/api/fotos/";

    private FotoUrlMapper() {
    }

    /** Null cuando el usuario no tiene foto. */
    static String aUrl(String fotoClave) {
        return fotoClave == null ? null : RUTA_FOTOS + fotoClave;
    }

    static FotoCitaResponse aFotoCita(CitaFoto foto) {
        return new FotoCitaResponse(foto.id(), "/api/citas/" + foto.citaId() + "/fotos/" + foto.id());
    }

    /** Lista vacía (nunca null) cuando la cita no tiene fotos. */
    static List<FotoCitaResponse> aFotosCita(List<CitaFoto> fotos) {
        return fotos.stream().map(FotoUrlMapper::aFotoCita).toList();
    }
}
