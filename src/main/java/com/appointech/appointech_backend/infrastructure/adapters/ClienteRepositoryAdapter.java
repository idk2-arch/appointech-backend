package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ClienteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository clienteJpaRepository;

    public ClienteRepositoryAdapter(ClienteJpaRepository clienteJpaRepository) {
        this.clienteJpaRepository = clienteJpaRepository;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteEntity entity = toEntity(cliente);
        ClienteEntity guardada = clienteJpaRepository.save(entity);
        return toDomain(guardada);
    }

    @Override
    public Optional<Cliente> buscarPorUsuarioId(Long usuarioId) {
        return clienteJpaRepository.findByUsuarioId(usuarioId).map(this::toDomain);
    }

    private ClienteEntity toEntity(Cliente cliente) {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(cliente.getId());
        entity.setDireccion(cliente.getDireccion());
        entity.setLatitud(cliente.getLatitud());
        entity.setLongitud(cliente.getLongitud());

        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setId(cliente.getUsuario().getId());
        entity.setUsuario(usuarioEntity);

        return entity;
    }

    private Cliente toDomain(ClienteEntity entity) {
        Cliente cliente = new Cliente();
        cliente.setId(entity.getId());
        cliente.setDireccion(entity.getDireccion());
        cliente.setLatitud(entity.getLatitud());
        cliente.setLongitud(entity.getLongitud());

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
        cliente.setUsuario(usuario);

        return cliente;
    }
}