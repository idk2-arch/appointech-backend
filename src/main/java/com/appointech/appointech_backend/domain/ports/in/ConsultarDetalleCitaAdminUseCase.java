package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;

public interface ConsultarDetalleCitaAdminUseCase {
    DetalleCitaCompleto detallar(Long citaId);
}
