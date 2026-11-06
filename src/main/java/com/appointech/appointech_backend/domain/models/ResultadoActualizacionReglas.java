package com.appointech.appointech_backend.domain.models;

/**
 * Reglas ya guardadas. {@code franjasOfrecidasInvalidadas} es true cuando cambió la duración o la jornada: las
 * franjas que algún cliente ya tenía en pantalla dejan de poder confirmarse y deben consultarse de nuevo.
 */
public record ResultadoActualizacionReglas(ReglasFranjas reglas, boolean franjasOfrecidasInvalidadas) {
}
