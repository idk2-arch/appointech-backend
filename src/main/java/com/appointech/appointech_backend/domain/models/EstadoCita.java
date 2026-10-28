package com.appointech.appointech_backend.domain.models;

import java.util.Optional;

public enum EstadoCita {
    PENDIENTE, CONFIRMADA, EN_CAMINO, EN_PROCESO, FINALIZADA, CANCELADA;

    /** Único estado al que el técnico puede avanzar la cita desde este; vacío si ya no avanza. */
    public Optional<EstadoCita> siguiente() {
        return switch (this) {
            case CONFIRMADA -> Optional.of(EN_CAMINO);
            case EN_CAMINO -> Optional.of(EN_PROCESO);
            case EN_PROCESO -> Optional.of(FINALIZADA);
            default -> Optional.empty();
        };
    }

    public boolean puedeAvanzarA(EstadoCita destino) {
        return siguiente().filter(s -> s == destino).isPresent();
    }

    /** FINALIZADA y CANCELADA no admiten ningún cambio. */
    public boolean esFinal() {
        return this == FINALIZADA || this == CANCELADA;
    }

    /** El cliente solo puede cancelar o reprogramar mientras el técnico aún no sale hacia el servicio. */
    public boolean admiteCancelacionOReprogramacionDelCliente() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
