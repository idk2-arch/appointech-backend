package com.appointech.appointech_backend.infrastructure.dto;

/**
 * Cuerpo de la petición: { "idToken": "eyJ..." }
 */
public record GoogleLoginRequest(String idToken) {}