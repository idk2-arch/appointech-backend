package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cotizacion;

public interface ConsultarCotizacionUseCase {
    Cotizacion consultar(Long citaId, String correoActor);
}
