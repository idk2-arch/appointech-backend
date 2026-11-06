package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;

/** Traduce los parámetros de texto de la URL a un filtro del dominio; un valor desconocido responde 400. */
final class SolicitudManualFiltroMapper {

    private SolicitudManualFiltroMapper() {
    }

    static FiltroSolicitudesManuales aFiltro(String motivo, Long especialidadId, String estado, Integer page, Integer size) {
        return new FiltroSolicitudesManuales(
                ParametrosDeConsulta.aEnum(MotivoSolicitudEspecial.class, "motivo", motivo),
                especialidadId,
                ParametrosDeConsulta.aEnum(EstadoCita.class, "estado", estado),
                page, size);
    }
}
