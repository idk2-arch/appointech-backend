package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Especialidad;

import java.util.List;
import java.util.Optional;

public interface EspecialidadRepositoryPort {
    List<Especialidad> listarTodas();
    List<Especialidad> listarActivas();
    List<Especialidad> buscarPorIds(List<Long> ids);
    Optional<Especialidad> buscarPorId(Long id);
    Optional<Especialidad> buscarPorNombre(String nombre);
    Especialidad guardar(Especialidad especialidad);
    long contarTecnicosAsignados(Long especialidadId);
}