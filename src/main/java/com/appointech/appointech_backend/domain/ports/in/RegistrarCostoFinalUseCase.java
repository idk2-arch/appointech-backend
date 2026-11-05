package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Pago;

import java.math.BigDecimal;

public interface RegistrarCostoFinalUseCase {
    /** El técnico asignado fija el costo final de una cita FINALIZADA. */
    Pago registrar(String correoTecnico, Long citaId, BigDecimal monto);
}
