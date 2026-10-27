package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.DatosActualizacionTecnico;
import com.appointech.appointech_backend.domain.models.Tecnico;

public interface ActualizarTecnicoUseCase {
    Tecnico actualizar(Long tecnicoId, DatosActualizacionTecnico datos);
}
