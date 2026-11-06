package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ResultadoActualizacionReglas;

import java.time.LocalTime;

public interface ActualizarReglasFranjasUseCase {
    /**
     * Reemplaza las reglas de franjas. Lanza {@code RadioCoberturaInvalidoException} o
     * {@code ReglasFranjasInvalidasException} si algún valor es inválido; en ese caso no cambia nada.
     * Aplica desde el siguiente cálculo de disponibilidad.
     */
    ResultadoActualizacionReglas actualizar(String correoAdministrador, Integer radioMaximoKm, Integer duracionBloqueMinutos,
                                            LocalTime horaInicioJornada, LocalTime horaFinJornada);
}
