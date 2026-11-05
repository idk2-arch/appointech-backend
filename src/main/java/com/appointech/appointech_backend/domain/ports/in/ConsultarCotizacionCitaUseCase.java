package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cotizacion;

public interface ConsultarCotizacionCitaUseCase {
    /** Cotización de la cita para el cliente dueño, el técnico asignado o un administrador; la deja guardada en el pago. */
    Cotizacion cotizar(String correo, Long citaId);
}
