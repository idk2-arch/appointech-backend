package com.appointech.appointech_backend.domain.exception;

public class CitaSinUbicacionException extends RuntimeException {
    public CitaSinUbicacionException(Long citaId) {
        super("La cita " + citaId + " no tiene coordenadas válidas; no se puede calcular la ruta");
    }
}
