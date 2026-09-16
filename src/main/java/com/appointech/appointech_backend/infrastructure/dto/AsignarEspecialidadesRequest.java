package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AsignarEspecialidadesRequest(
        @NotEmpty(message = "Debe seleccionar al menos una especialidad")
        List<Long> especialidadIds
) {
}