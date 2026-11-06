package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.CalificacionesDelTecnico;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionRecibidaResponse;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionResponse;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionesTecnicoResponse;

final class CalificacionMapper {

    private CalificacionMapper() {
    }

    static CalificacionResponse aRespuesta(Calificacion c) {
        return new CalificacionResponse(c.citaId(), c.puntuacion(), c.comentario(), c.creadaEn());
    }

    /** Null si la cita aún no tiene calificación. */
    static CalificacionAdminResponse aRespuestaAdmin(Calificacion c) {
        return c == null ? null : new CalificacionAdminResponse(c.puntuacion(), c.comentario(), c.creadaEn());
    }

    static CalificacionesTecnicoResponse aRespuesta(CalificacionesDelTecnico c) {
        return new CalificacionesTecnicoResponse(c.promedio(), c.total(),
                c.calificaciones().stream().map(CalificacionMapper::aRecibida).toList());
    }

    private static CalificacionRecibidaResponse aRecibida(CalificacionRecibida r) {
        return new CalificacionRecibidaResponse(r.citaId(), r.puntuacion(), r.comentario(), r.fecha(), r.clienteNombre());
    }
}
