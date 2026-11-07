package com.appointech.appointech_backend.domain.models;

/** Qué ocurrió; el frontend puede usarlo para elegir el ícono o el destino del clic. */
public enum TipoNotificacion {
    SOLICITUD_RECIBIDA,
    CITA_CONFIRMADA,
    NUEVA_CITA_ASIGNADA,
    CITA_CANCELADA,
    CITA_FINALIZADA,
    PAGO_REGISTRADO,
    SOLICITUD_FUERA_DE_COBERTURA,
    SOLICITUD_SIN_TECNICOS
}
