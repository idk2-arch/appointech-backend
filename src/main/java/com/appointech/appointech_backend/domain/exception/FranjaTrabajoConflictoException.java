package com.appointech.appointech_backend.domain.exception;

/**
 * La franja choca con algo que ya existe: otra franja, una no disponibilidad del técnico o citas ya agendadas
 * (se responde 409). El mensaje dice con qué choca, con fechas y horas.
 */
public class FranjaTrabajoConflictoException extends RuntimeException {
    public FranjaTrabajoConflictoException(String mensaje) {
        super(mensaje);
    }
}
