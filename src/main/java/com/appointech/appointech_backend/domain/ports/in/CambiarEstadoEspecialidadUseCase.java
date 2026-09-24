package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Especialidad;

public interface CambiarEstadoEspecialidadUseCase {
    Especialidad cambiarEstado(Long id, boolean activo);
}