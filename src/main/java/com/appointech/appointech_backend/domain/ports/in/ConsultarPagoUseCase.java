package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Pago;

public interface ConsultarPagoUseCase {
    Pago consultar(String correo, Long citaId);
}
