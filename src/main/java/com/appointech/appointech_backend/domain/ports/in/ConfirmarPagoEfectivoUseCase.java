package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Pago;

public interface ConfirmarPagoEfectivoUseCase {
    /** El técnico asignado confirma que recibió el efectivo; genera el comprobante. */
    Pago confirmar(String correoTecnico, Long citaId);
}
