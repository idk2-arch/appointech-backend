package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Especialidad;

import java.util.List;

public interface EspecialidadRepositoryPort {
    List<Especialidad> listarTodas();
    List<Especialidad> buscarPorIds(List<Long> ids);
}