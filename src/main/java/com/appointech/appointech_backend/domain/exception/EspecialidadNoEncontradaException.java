package com.appointech.appointech_backend.domain.exception;

import java.util.List;

public class EspecialidadNoEncontradaException extends RuntimeException {
    public EspecialidadNoEncontradaException(List<Long> idsNoEncontrados) {
        super("No se encontraron las especialidades con id: " + idsNoEncontrados);
    }
}