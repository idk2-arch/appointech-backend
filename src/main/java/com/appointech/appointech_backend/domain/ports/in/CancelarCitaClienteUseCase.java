package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;

public interface CancelarCitaClienteUseCase {
    /** El cliente dueño cancela su cita PENDIENTE o CONFIRMADA; el motivo es opcional. */
    Cita cancelar(String correoCliente, Long citaId, String motivo);
}
