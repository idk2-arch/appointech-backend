package com.appointech.appointech_backend.domain.models;

/** Respuesta de la pasarela al iniciar un cobro: la referencia que usó y la URL a la que se envía al cliente. */
public record InicioPago(String referencia, String urlPago) {
}
