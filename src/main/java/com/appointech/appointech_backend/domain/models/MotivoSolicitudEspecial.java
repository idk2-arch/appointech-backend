package com.appointech.appointech_backend.domain.models;

/** Por qué una solicitud no sigue el flujo normal de franjas y la debe atender un administrador. */
public enum MotivoSolicitudEspecial {
    /** No hay ningún técnico activo con la especialidad pedida (RF28). */
    SIN_TECNICOS_ESPECIALIDAD,
    /** Hay técnicos con la especialidad, pero ninguno cubre la dirección. */
    FUERA_DE_COBERTURA
}
