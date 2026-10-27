package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;

public interface ObtenerDetalleCitaUseCase {
    DetalleCita detallar(Cita cita);
}
