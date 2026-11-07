package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FranjaTrabajoInvalidaException;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.infrastructure.dto.FranjaTrabajoResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

final class FranjaTrabajoMapper {

    private FranjaTrabajoMapper() {
    }

    static FranjaTrabajoResponse aRespuesta(FranjaTrabajoConOcupacion f) {
        FranjaTrabajo franja = f.franja();
        return new FranjaTrabajoResponse(franja.id(), franja.tecnicoId(), franja.fecha().toString(),
                franja.horaInicio().toString(), franja.horaFin().toString(), f.bloques(), f.bloquesOcupados(), franja.creadaEn());
    }

    /** Obligatoria, en formato yyyy-MM-dd; si falta o está mal escrita es un 400. */
    static LocalDate aFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new FranjaTrabajoInvalidaException("La fecha es obligatoria (formato yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new FranjaTrabajoInvalidaException("La fecha debe tener el formato yyyy-MM-dd");
        }
    }

    /** Obligatoria, en formato HH:mm; si falta o está mal escrita es un 400. */
    static LocalTime aHora(String campo, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new FranjaTrabajoInvalidaException(campo + " es obligatoria (formato HH:mm)");
        }
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new FranjaTrabajoInvalidaException(campo + " debe tener el formato HH:mm");
        }
    }
}
