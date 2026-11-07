package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.infrastructure.dto.NotificacionResponse;

final class NotificacionMapper {

    private NotificacionMapper() {
    }

    static NotificacionResponse aRespuesta(Notificacion notificacion) {
        return new NotificacionResponse(notificacion.id(), notificacion.tipo().name(), notificacion.titulo(),
                notificacion.mensaje(), notificacion.citaId(), notificacion.leida(), notificacion.creadaEn());
    }

    /** Falso si falta; "true" o "false" en cualquier mayúscula. Otro valor es un filtro inválido (400). */
    static boolean aSoloNoLeidas(String texto) {
        Boolean valor = ParametrosDeConsulta.aBooleano("soloNoLeidas", texto);
        return valor != null && valor;
    }

    static PaginacionOpcional aPaginacion(Integer page, Integer size) {
        return PaginacionOpcional.de(page, size);
    }
}
