package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EspecialidadRepositoryAdapter implements EspecialidadRepositoryPort {

    private final EspecialidadJpaRepository especialidadJpaRepository;

    public EspecialidadRepositoryAdapter(EspecialidadJpaRepository especialidadJpaRepository) {
        this.especialidadJpaRepository = especialidadJpaRepository;
    }

    @Override
    public List<Especialidad> listarTodas() {
        return especialidadJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Especialidad> buscarPorIds(List<Long> ids) {
        return especialidadJpaRepository.findAllById(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    private Especialidad toDomain(EspecialidadEntity entity) {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(entity.getId());
        especialidad.setNombre(entity.getNombre());
        especialidad.setDescripcion(entity.getDescripcion());
        return especialidad;
    }
}