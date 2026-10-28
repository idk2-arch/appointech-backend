package com.appointech.appointech_backend.domain.models;

import java.util.List;

/**
 * Datos del perfil del usuario autenticado. La dirección es la del cliente o la base del técnico
 * (null para el administrador); el radio y las especialidades solo existen para el técnico.
 */
public record PerfilPropio(Usuario usuario, String direccion, Integer radioCoberturaKm,
                           List<Especialidad> especialidades) {
}
