package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.infrastructure.dto.AsignacionManualResponse;

/** Trazabilidad de la asignación manual para las respuestas de cita. */
final class AsignacionManualMapper {

    private AsignacionManualMapper() {
    }

    /** Null si la cita no se asignó manualmente; en las filas antiguas el objeto existe pero con datos nulos. */
    static AsignacionManualResponse aRespuesta(DetalleCita detalle) {
        Cita cita = detalle.cita();
        if (!cita.isAsignacionManual()) {
            return null;
        }
        return new AsignacionManualResponse(cita.getAsignadaPorId(), detalle.asignadaPorNombre(), cita.getAsignadaEn());
    }
}
