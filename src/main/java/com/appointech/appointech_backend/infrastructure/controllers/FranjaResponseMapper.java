package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.infrastructure.dto.FranjaDisponibleResponse;

final class FranjaResponseMapper {

    private FranjaResponseMapper() {
    }

    static FranjaDisponibleResponse aRespuesta(FranjaDisponible franja) {
        return new FranjaDisponibleResponse(
                franja.getTecnicoId(), franja.getInicio(), franja.getFin(),
                Math.round(franja.getDistanciaKm() * 100.0) / 100.0,
                franja.getTecnicoNombre(), franja.getTecnicoApellido()
        );
    }
}
