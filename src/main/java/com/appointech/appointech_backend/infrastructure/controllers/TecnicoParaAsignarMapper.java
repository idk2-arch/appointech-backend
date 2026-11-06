package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.models.TecnicoDisponible;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoParaAsignarResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

final class TecnicoParaAsignarMapper {

    private TecnicoParaAsignarMapper() {
    }

    static TecnicoParaAsignarResponse aRespuesta(TecnicoDisponible t) {
        return new TecnicoParaAsignarResponse(t.tecnicoId(), t.nombre(), t.apellido(), t.telefono(),
                FotoUrlMapper.aUrl(t.fotoClave()),
                t.distanciaKm() == null ? null : Math.round(t.distanciaKm() * 100.0) / 100.0);
    }

    /** Extremo del horario en formato ISO (2030-05-06T09:00:00), obligatorio; faltante o mal escrito es 400. */
    static LocalDateTime aFechaHora(String parametro, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new FranjaInvalidaException(parametro + " es obligatorio (formato 2030-05-06T09:00:00)");
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new FranjaInvalidaException(parametro + " debe tener el formato 2030-05-06T09:00:00");
        }
    }
}
