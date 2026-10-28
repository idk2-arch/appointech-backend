package com.appointech.appointech_backend.infrastructure.dto;

/** Foto del electrodoméstico de una cita; url es la ruta autenticada /api/citas/{citaId}/fotos/{id}. */
public record FotoCitaResponse(Long id, String url) {
}
