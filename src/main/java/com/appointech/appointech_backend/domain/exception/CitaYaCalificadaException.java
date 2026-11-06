package com.appointech.appointech_backend.domain.exception;

/** La cita ya tiene calificación; no se edita ni se repite (se responde 409). */
public class CitaYaCalificadaException extends RuntimeException {
    public CitaYaCalificadaException() {
        super("Esta cita ya fue calificada");
    }
}
