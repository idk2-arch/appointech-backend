package com.appointech.appointech_backend.domain.models;

import java.util.List;

/**
 * Franjas ofrecidas; si no hay ninguna, {@code motivo} explica por qué (null cuando sí hay franjas).
 * {@code telefonoContactoAdmin} solo viene cuando el motivo es FUERA_DE_COBERTURA y hay un teléfono configurado.
 */
public record ResultadoFranjas(List<FranjaDisponible> franjas, MotivoSinFranjas motivo, String telefonoContactoAdmin) {

    public ResultadoFranjas(List<FranjaDisponible> franjas, MotivoSinFranjas motivo) {
        this(franjas, motivo, null);
    }
}
