package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Pago;

import java.math.BigDecimal;

public interface RegistrarCostoFinalUseCase {
    Pago registrar(Long citaId, BigDecimal montoFinal, String correoActor);
}
