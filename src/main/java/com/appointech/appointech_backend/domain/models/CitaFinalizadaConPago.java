package com.appointech.appointech_backend.domain.models;

/** Cita finalizada del técnico con los datos de su agenda y la situación de su cobro. */
public record CitaFinalizadaConPago(CitaConCliente cita, EstadoCobro cobro) {
}
