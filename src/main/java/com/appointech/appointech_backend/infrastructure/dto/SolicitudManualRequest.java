package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudManualRequest(
        @NotNull(message = "La especialidad es obligatoria")
        Long especialidadId,

        @NotBlank(message = "La descripción del problema es obligatoria")
        String descripcionProblema,

        @NotBlank(message = "La dirección del servicio es obligatoria")
        String direccionServicio,

        @NotNull(message = "La latitud es obligatoria")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        Double longitud,

        /** Obligatorio solo si la cuenta del cliente no tiene teléfono registrado. */
        String telefonoContacto
) {
}
