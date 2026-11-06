package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Conversión de los parámetros de texto de la URL a enums y fechas; un valor inválido es un filtro inválido (400). */
final class ParametrosDeConsulta {

    private ParametrosDeConsulta() {
    }

    /** Null si el texto falta o está en blanco; mayúsculas o minúsculas y espacios no importan. */
    static <E extends Enum<E>> E aEnum(Class<E> tipo, String parametro, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FiltroCitasInvalidoException("Valor inválido para " + parametro + ": " + texto);
        }
    }

    /** Null si el texto falta o está en blanco; el formato es yyyy-MM-dd. */
    static LocalDate aFecha(String parametro, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new FiltroCitasInvalidoException(parametro + " debe tener el formato yyyy-MM-dd");
        }
    }
}
