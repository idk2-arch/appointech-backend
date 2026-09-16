package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.TecnicoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.TecnicoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TecnicoRepositoryAdapter implements TecnicoRepositoryPort {

    private final TecnicoJpaRepository tecnicoJpaRepository;
    private final EspecialidadJpaRepository especialidadJpaRepository;

    public TecnicoRepositoryAdapter(TecnicoJpaRepository tecnicoJpaRepository,
                                    EspecialidadJpaRepository especialidadJpaRepository) {
        this.tecnicoJpaRepository = tecnicoJpaRepository;
        this.especialidadJpaRepository = especialidadJpaRepository;
    }

    @Override
    public Tecnico guardar(Tecnico tecnico) {
        TecnicoEntity entity = toEntity(tecnico);
        TecnicoEntity guardada = tecnicoJpaRepository.save(entity);
        return toDomain(guardada);
    }

    @Override
    public Optional<Tecnico> buscarPorUsuarioId(Long usuarioId) {
        return tecnicoJpaRepository.findByUsuarioId(usuarioId).map(this::toDomain);
    }

    @Override
    public Optional<Tecnico> buscarPorId(Long id) {
        return tecnicoJpaRepository.findById(id).map(this::toDomain);
    }

    private TecnicoEntity toEntity(Tecnico tecnico) {
        TecnicoEntity entity = new TecnicoEntity();
        entity.setId(tecnico.getId());
        entity.setDireccionBase(tecnico.getDireccionBase());
        entity.setLatitudBase(tecnico.getLatitudBase());
        entity.setLongitudBase(tecnico.getLongitudBase());
        entity.setRadioCoberturaKm(tecnico.getRadioCoberturaKm());

        List<EspecialidadEntity> especialidades = new ArrayList<>();
        if (tecnico.getEspecialidades() != null) {
            List<Long> ids = tecnico.getEspecialidades().stream().map(Especialidad::getId).toList();
            especialidades = especialidadJpaRepository.findAllById(ids);
        }
        entity.setEspecialidades(especialidades);

        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setId(tecnico.getUsuario().getId());
        entity.setUsuario(usuarioEntity);

        return entity;
    }

    private Tecnico toDomain(TecnicoEntity entity) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(entity.getId());
        tecnico.setDireccionBase(entity.getDireccionBase());
        tecnico.setLatitudBase(entity.getLatitudBase());
        tecnico.setLongitudBase(entity.getLongitudBase());
        tecnico.setRadioCoberturaKm(entity.getRadioCoberturaKm());

        List<Especialidad> especialidades = new ArrayList<>();
        if (entity.getEspecialidades() != null) {
            especialidades = entity.getEspecialidades().stream().map(this::toDomainEspecialidad).toList();
        }
        tecnico.setEspecialidades(especialidades);

        UsuarioEntity usuarioEntity = entity.getUsuario();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioEntity.getId());
        usuario.setNombre(usuarioEntity.getNombre());
        usuario.setApellido(usuarioEntity.getApellido());
        usuario.setCorreo(usuarioEntity.getCorreo());
        usuario.setTelefono(usuarioEntity.getTelefono());
        if (usuarioEntity.getTipoDocumento() != null) {
            usuario.setTipoDocumento(TipoDocumento.valueOf(usuarioEntity.getTipoDocumento().name()));
        }
        usuario.setNumeroDocumento(usuarioEntity.getNumeroDocumento());
        usuario.setRol(RolUsuario.valueOf(usuarioEntity.getRol().name()));
        usuario.setActivo(usuarioEntity.isActivo());
        usuario.setCreadoEn(usuarioEntity.getCreadoEn());
        tecnico.setUsuario(usuario);

        return tecnico;
    }

    private Especialidad toDomainEspecialidad(EspecialidadEntity entity) {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(entity.getId());
        especialidad.setNombre(entity.getNombre());
        especialidad.setDescripcion(entity.getDescripcion());
        return especialidad;
    }
}