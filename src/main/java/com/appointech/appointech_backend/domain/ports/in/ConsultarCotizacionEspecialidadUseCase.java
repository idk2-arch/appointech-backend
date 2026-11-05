package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Cotizacion;

public interface ConsultarCotizacionEspecialidadUseCase {
    /** Valor estimado de una especialidad activa, sin necesidad de cita. */
    Cotizacion cotizar(Long especialidadId);
}
