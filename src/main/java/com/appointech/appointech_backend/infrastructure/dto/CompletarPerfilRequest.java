package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompletarPerfilRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,

        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        String numeroDocumento,

        String direccion
) {
}