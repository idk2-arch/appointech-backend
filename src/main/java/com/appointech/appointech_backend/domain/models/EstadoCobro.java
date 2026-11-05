package com.appointech.appointech_backend.domain.models;

/** Situación del cobro de una cita vista por el técnico: aún sin costo final, por cobrar o ya cobrado. */
public enum EstadoCobro {
    SIN_COSTO_FINAL,
    PENDIENTE,
    PAGADO
}
