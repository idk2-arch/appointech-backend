package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Especialidad;

public interface CrearEspecialidadUseCase {
    Especialidad crear(String nombre, String descripcion);
}