package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

public record ComprobantePago(Long id, Long pagoId, LocalDateTime emitidoEn) {

    /** Número legible derivado del id consecutivo, p. ej. COMP-000001. */
    public String numero() {
        return String.format("COMP-%06d", id);
    }
}
