package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.ComprobantePago;

import java.util.Optional;

public interface ComprobantePagoRepositoryPort {
    ComprobantePago guardar(ComprobantePago comprobante);
    Optional<ComprobantePago> buscarPorPagoId(Long pagoId);
}
