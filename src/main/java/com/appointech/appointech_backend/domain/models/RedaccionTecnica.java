package com.appointech.appointech_backend.domain.models;

/** Descripción del cliente reescrita en lenguaje técnico; si no hay servicio, {@link #noDisponible()}. */
public record RedaccionTecnica(boolean disponible, String textoTecnico) {

    public static final String MENSAJE_NO_DISPONIBLE =
            "La redacción técnica no está disponible en este momento; puedes enviar tu descripción tal como está";

    public static RedaccionTecnica conTexto(String textoTecnico) {
        return new RedaccionTecnica(true, textoTecnico);
    }

    public static RedaccionTecnica noDisponible() {
        return new RedaccionTecnica(false, MENSAJE_NO_DISPONIBLE);
    }
}
