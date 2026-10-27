package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Tecnico;

public interface ActualizarUbicacionTecnicoUseCase {
    Tecnico actualizar(Long tecnicoId, Double latitud, Double longitud);
}
