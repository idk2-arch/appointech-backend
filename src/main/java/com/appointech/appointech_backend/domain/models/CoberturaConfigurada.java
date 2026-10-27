package com.appointech.appointech_backend.domain.models;

/** Configuración de cobertura: límite único de distancia y teléfono de WhatsApp del administrador (puede ser null). */
public record CoberturaConfigurada(int radioMaximoKm, String telefonoWhatsappAdmin) {
}
