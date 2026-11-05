package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Pago;

import java.util.Optional;

public interface PagoRepositoryPort {
    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    Optional<Pago> buscarPorCitaId(Long citaId);
}
