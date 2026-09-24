package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Especialidad;

public interface ActualizarEspecialidadUseCase {
    Especialidad actualizar(Long id, String nombre, String descripcion);
}