package com.appointech.appointech_backend.domain.models;

/** Orientación generada por IA para el cliente; si no hay servicio, {@link #noDisponible()} y el flujo sigue igual. */
public record PreDiagnostico(boolean disponible, String sugerencia) {

    public static final String MENSAJE_NO_DISPONIBLE =
            "El pre-diagnóstico no está disponible en este momento; un técnico revisará tu solicitud";

    public static PreDiagnostico conSugerencia(String sugerencia) {
        return new PreDiagnostico(true, sugerencia);
    }

    public static PreDiagnostico noDisponible() {
        return new PreDiagnostico(false, MENSAJE_NO_DISPONIBLE);
    }
}
