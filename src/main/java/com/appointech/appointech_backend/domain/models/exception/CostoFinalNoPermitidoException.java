package com.appointech.appointech_backend.domain.models.exception;

import com.appointech.appointech_backend.domain.models.EstadoCita;

public class CostoFinalNoPermitidoException extends RuntimeException {
    public CostoFinalNoPermitidoException(EstadoCita actual) {
        super("Solo se puede registrar el costo final de una cita finalizada; la cita está '"
                + actual.getEtiqueta() + "'");
    }
}
