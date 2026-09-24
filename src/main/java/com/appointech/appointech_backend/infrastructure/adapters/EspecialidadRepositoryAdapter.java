package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EspecialidadRepositoryAdapter implements EspecialidadRepositoryPort {

    private final EspecialidadJpaRepository especialidadJpaRepository;

    public EspecialidadRepositoryAdapter(EspecialidadJpaRepository especialidadJpaRepository) {
        this.especialidadJpaRepository = especialidadJpaRepository;
    }

    @Override
    public List<Especialidad> listarTodas() {
        return especialidadJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Especialidad> listarActivas() {
        return especialidadJpaRepository.findByActivoTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Especialidad> buscarPorIds(List<Long> ids) {
        return especialidadJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Especialidad> buscarPorId(Long id) {
        return especialidadJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Especialidad> buscarPorNombre(String nombre) {
        return especialidadJpaRepository.findByNombre(nombre).map(this::toDomain);
    }

    @Override
    public Especialidad guardar(Especialidad especialidad) {
        EspecialidadEntity entity = toEntity(especialidad);
        EspecialidadEntity guardada = especialidadJpaRepository.save(entity);
        return toDomain(guardada);
    }

    @Override
    public long contarTecnicosAsignados(Long especialidadId) {
        return especialidadJpaRepository.contarTecnicosPorEspecialidad(especialidadId);
    }

    private EspecialidadEntity toEntity(Especialidad especialidad) {
        EspecialidadEntity entity = new EspecialidadEntity();
        entity.setId(especialidad.getId());
        entity.setNombre(especialidad.getNombre());
        entity.setDescripcion(especialidad.getDescripcion());
        entity.setActivo(especialidad.isActivo());
        return entity;
    }

    private Especialidad toDomain(EspecialidadEntity entity) {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(entity.getId());
        especialidad.setNombre(entity.getNombre());
        especialidad.setDescripcion(entity.getDescripcion());
        especialidad.setActivo(entity.isActivo());
        return especialidad;
    }
}