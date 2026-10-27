package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarUbicacionTecnicoRequest(
        @NotNull(message = "La latitud es obligatoria")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        Double longitud
) {
}
