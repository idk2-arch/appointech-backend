package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;

public interface ActualizarEstadoCitaUseCase {
    /** El técnico asignado avanza su cita al siguiente estado permitido. */
    Cita actualizar(String correoTecnico, Long citaId, EstadoCita nuevoEstado);
}
