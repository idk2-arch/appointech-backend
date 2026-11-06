package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;

/** Traduce los parámetros de texto de la URL a un filtro del dominio; un valor desconocido responde 400. */
final class SolicitudManualFiltroMapper {

    private SolicitudManualFiltroMapper() {
    }

    static FiltroSolicitudesManuales aFiltro(String motivo, Long especialidadId, String estado, Integer page, Integer size) {
        return new FiltroSolicitudesManuales(
                aValor(MotivoSolicitudEspecial.class, "motivo", motivo),
                especialidadId,
                aValor(EstadoCita.class, "estado", estado),
                page, size);
    }

    private static <E extends Enum<E>> E aValor(Class<E> tipo, String parametro, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FiltroCitasInvalidoException("Valor inválido para " + parametro + ": " + texto);
        }
    }
}
