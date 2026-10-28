package com.appointech.appointech_backend.domain.exception;

/** El usuario no es el cliente dueño, el técnico asignado ni administrador de la cita (se responde 403). */
public class AccesoCitaDenegadoException extends RuntimeException {
    public AccesoCitaDenegadoException() {
        super("No tienes permisos sobre esta cita");
    }
}
