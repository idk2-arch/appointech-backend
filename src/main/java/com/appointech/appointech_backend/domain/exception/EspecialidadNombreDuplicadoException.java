package com.appointech.appointech_backend.domain.exception;

public class EspecialidadNombreDuplicadoException extends RuntimeException {
    public EspecialidadNombreDuplicadoException(String nombre) {
        super("Ya existe una especialidad con el nombre: " + nombre);
    }
}