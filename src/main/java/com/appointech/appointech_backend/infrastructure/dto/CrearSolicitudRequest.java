package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearSolicitudRequest(
        @NotNull(message = "La especialidad es obligatoria")
        Long especialidadId,

        @NotBlank(message = "La descripción del problema es obligatoria")
        String descripcionProblema,

        @NotBlank(message = "La dirección del servicio es obligatoria")
        String direccionServicio,

        @NotNull(message = "La latitud es obligatoria")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        Double longitud
) {
}
