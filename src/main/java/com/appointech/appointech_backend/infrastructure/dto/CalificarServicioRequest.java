package com.appointech.appointech_backend.infrastructure.dto;

/** Puntuación de 1 a 5 y comentario opcional de hasta 500 caracteres; las reglas las valida el dominio (400). */
public record CalificarServicioRequest(Integer puntuacion, String comentario) {
}
