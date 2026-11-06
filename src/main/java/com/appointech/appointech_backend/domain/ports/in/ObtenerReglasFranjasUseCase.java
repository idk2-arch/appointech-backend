package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ReglasFranjas;

public interface ObtenerReglasFranjasUseCase {
    /** Reglas vigentes de franjas (radio, duración del bloque y jornada). */
    ReglasFranjas obtener();
}
