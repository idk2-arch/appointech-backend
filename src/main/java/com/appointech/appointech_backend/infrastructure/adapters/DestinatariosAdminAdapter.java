package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.DestinatariosAdminPort;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.UsuarioJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DestinatariosAdminAdapter implements DestinatariosAdminPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public DestinatariosAdminAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public List<Long> idsAdministradoresActivos() {
        return usuarioJpaRepository.idsActivosPorRol(RolUsuarioEntity.ADMINISTRADOR);
    }
}
