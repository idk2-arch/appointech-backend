package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.RedaccionTecnica;

public interface RedactarDescripcionTecnicaUseCase {
    RedaccionTecnica redactar(String descripcionProblema, String especialidad);
}
