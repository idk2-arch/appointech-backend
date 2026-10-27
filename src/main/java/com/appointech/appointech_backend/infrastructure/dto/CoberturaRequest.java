package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

/**
 * El rango del radio (1 a 100 km) y el formato del teléfono los valida el dominio para que el mensaje sea el mismo
 * en cualquier entrada. {@code telefonoWhatsappAdmin}: ausente/null no lo modifica, vacío lo borra.
 */
public record CoberturaRequest(
        @NotNull(message = "El radio máximo de cobertura es obligatorio")
        Integer radioMaximoKm,

        String telefonoWhatsappAdmin
) {
}
