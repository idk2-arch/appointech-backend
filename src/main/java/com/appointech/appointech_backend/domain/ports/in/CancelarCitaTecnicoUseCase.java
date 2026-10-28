package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface CancelarCitaTecnicoUseCase {
    /** El técnico asignado cancela la cita; el motivo es obligatorio. */
    Cita cancelar(String correoTecnico, Long citaId, String motivo);
}
