package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Especialidad;

import java.util.List;

public interface ListarEspecialidadesUseCase {
    List<Especialidad> listar();
}