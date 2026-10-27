package com.appointech.appointech_backend.domain.models;

/** Por qué no se ofrecen franjas para una especialidad y una ubicación. */
public enum MotivoSinFranjas {
    /** No hay técnicos activos con esa especialidad. */
    SIN_TECNICOS_ESPECIALIDAD,
    /** Hay técnicos con la especialidad, pero ninguno llega dentro del límite de distancia. */
    FUERA_DE_COBERTURA,
    /** Hay cobertura, pero no horarios libres ese día. */
    SIN_HORARIOS
}
