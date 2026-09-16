package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Tecnico;

import java.util.List;

public interface AsignarEspecialidadesTecnicoUseCase {
    Tecnico asignar(Long tecnicoId, List<Long> especialidadIds);
}