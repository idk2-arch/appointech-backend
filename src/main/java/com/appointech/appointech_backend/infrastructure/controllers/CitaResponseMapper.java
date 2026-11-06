package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.infrastructure.dto.AsignacionManualResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;

final class CitaResponseMapper {

    private CitaResponseMapper() {
    }

    /** Para cliente y técnico: indica si se asignó manualmente, pero no quién (no se expone al administrador). */
    static CitaResponse aRespuesta(DetalleCita detalle) {
        return construir(detalle, null);
    }

    /** Para el administrador: incluye quién y cuándo asignó manualmente. */
    static CitaResponse aRespuestaAdmin(DetalleCita detalle) {
        return construir(detalle, AsignacionManualMapper.aRespuesta(detalle));
    }

    private static CitaResponse construir(DetalleCita detalle, AsignacionManualResponse asignacion) {
        Cita cita = detalle.cita();
        return new CitaResponse(
                cita.getId(), cita.getTecnicoId(), cita.getEspecialidadId(),
                cita.getDescripcionProblema(), cita.getDireccionServicio(),
                cita.getFechaHoraInicio(), cita.getFechaHoraFin(), cita.getEstado().name(),
                detalle.tecnicoNombre(), detalle.tecnicoApellido(), detalle.tecnicoTelefono(),
                detalle.especialidadNombre(),
                FotoUrlMapper.aUrl(detalle.tecnicoFotoClave()),
                cita.isAgendamientoManual(),
                cita.getMotivoSolicitudEspecial() == null ? null : cita.getMotivoSolicitudEspecial().name(),
                cita.isAsignacionManual(),
                asignacion
        );
    }
}
