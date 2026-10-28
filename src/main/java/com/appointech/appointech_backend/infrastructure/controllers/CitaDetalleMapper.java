package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.DiagnosticoResponse;
import com.appointech.appointech_backend.infrastructure.dto.HistorialEstadoResponse;

final class CitaDetalleMapper {

    private CitaDetalleMapper() {
    }

    static CitaDetalleResponse aRespuesta(DetalleCitaCompleto completo) {
        DetalleCita detalle = completo.detalle();
        Cita cita = detalle.cita();
        return new CitaDetalleResponse(
                cita.getId(), cita.getEstado().name(), cita.getTecnicoId(),
                detalle.tecnicoNombre(), detalle.tecnicoApellido(), detalle.tecnicoTelefono(),
                detalle.especialidadNombre(), cita.getDescripcionProblema(), cita.getDireccionServicio(),
                cita.getFechaHoraInicio(), cita.getFechaHoraFin(),
                aDiagnostico(cita),
                completo.historial().stream().map(CitaDetalleMapper::aHistorial).toList(),
                FotoUrlMapper.aUrl(detalle.tecnicoFotoClave()));
    }

    /** Null mientras el técnico no haya registrado un diagnóstico. */
    static DiagnosticoResponse aDiagnostico(Cita cita) {
        if (cita.getDiagnostico() == null) {
            return null;
        }
        return new DiagnosticoResponse(cita.getDiagnostico(), cita.getRecomendaciones(), cita.getFechaDiagnostico());
    }

    private static HistorialEstadoResponse aHistorial(HistorialEstadoCita h) {
        return new HistorialEstadoResponse(
                h.estadoAnterior() == null ? null : h.estadoAnterior().name(),
                h.estadoNuevo().name(), h.fecha(), h.actor(), h.motivo());
    }
}
