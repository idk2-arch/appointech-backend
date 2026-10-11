package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;

final class CitaResponseMapper {

    private CitaResponseMapper() {
    }

    static CitaResponse aRespuesta(Cita cita) {
        return new CitaResponse(
                cita.getId(), cita.getTecnicoId(), cita.getEspecialidadId(),
                cita.getDescripcionProblema(), cita.getDireccionServicio(),
                cita.getFechaHoraInicio(), cita.getFechaHoraFin(), cita.getEstado().name()
        );
    }
}
