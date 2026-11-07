package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Tecnico;

public interface CambiarRadioAtencionUseCase {

    /** Fija la distancia máxima (1 a 100 km) a la que el técnico atiende desde su base. */
    Tecnico cambiar(String correoAdministrador, Long tecnicoId, Integer radioMaximoKm);
}
