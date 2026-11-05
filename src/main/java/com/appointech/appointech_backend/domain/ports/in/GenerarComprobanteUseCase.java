package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ComprobantePago;

public interface GenerarComprobanteUseCase {
    ComprobantePago generar(Long citaId, String correoActor);
    ComprobantePago consultar(Long citaId, String correoActor);
}
