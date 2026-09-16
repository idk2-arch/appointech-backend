package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.TipoDocumentoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.UsuarioJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = toEntity(usuario);
        UsuarioEntity guardada = usuarioJpaRepository.save(entity);
        return toDomain(guardada);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreo(correo).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private UsuarioEntity toEntity(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(usuario.getId());
        entity.setNombre(usuario.getNombre());
        entity.setApellido(usuario.getApellido());
        entity.setCorreo(usuario.getCorreo());
        entity.setContrasena(usuario.getContrasena());
        entity.setTelefono(usuario.getTelefono());
        if (usuario.getTipoDocumento() != null) {
            entity.setTipoDocumento(TipoDocumentoEntity.valueOf(usuario.getTipoDocumento().name()));
        }
        entity.setNumeroDocumento(usuario.getNumeroDocumento());
        entity.setRol(RolUsuarioEntity.valueOf(usuario.getRol().name()));
        entity.setActivo(usuario.isActivo());
        entity.setCreadoEn(usuario.getCreadoEn());
        return entity;
    }

    private Usuario toDomain(UsuarioEntity entity) {
        Usuario usuario = new Usuario();
        usuario.setId(entity.getId());
        usuario.setNombre(entity.getNombre());
        usuario.setApellido(entity.getApellido());
        usuario.setCorreo(entity.getCorreo());
        usuario.setContrasena(entity.getContrasena());
        usuario.setTelefono(entity.getTelefono());
        if (entity.getTipoDocumento() != null) {
            usuario.setTipoDocumento(TipoDocumento.valueOf(entity.getTipoDocumento().name()));
        }
        usuario.setNumeroDocumento(entity.getNumeroDocumento());
        usuario.setRol(RolUsuario.valueOf(entity.getRol().name()));
        usuario.setActivo(entity.isActivo());
        usuario.setCreadoEn(entity.getCreadoEn());
        return usuario;
    }
}