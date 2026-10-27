package com.appointech.appointech_backend.infrastructure.dto;

import java.util.List;

/**
 * Franjas ofrecidas y, si no hay ninguna, el motivo (SIN_TECNICOS_ESPECIALIDAD, FUERA_DE_COBERTURA o SIN_HORARIOS).
 * {@code telefonoContactoAdmin} solo viene con FUERA_DE_COBERTURA y si hay un teléfono configurado.
 */
public record FranjasConMotivoResponse(
        List<FranjaDisponibleResponse> franjas,
        String motivoSinFranjas,
        String telefonoContactoAdmin
) {
}
