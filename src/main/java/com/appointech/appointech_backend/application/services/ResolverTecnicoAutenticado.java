package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.PerfilTecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Traduce el correo del JWT al técnico. Es la única razón de cambio de esta clase:
 * cómo se identifica al técnico que hace la petición.
 */
@Component
public class ResolverTecnicoAutenticado {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public ResolverTecnicoAutenticado(UsuarioRepositoryPort usuarioRepositoryPort,
                                      TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    public Tecnico resolver(String correo) {
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new PerfilTecnicoNoEncontradoException(correo));
        return tecnicoRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new PerfilTecnicoNoEncontradoException(correo));
    }

    /** Igual que resolver, pero toma el bloqueo del técnico para serializar cambios sobre su agenda. */
    public Tecnico resolverBloqueando(String correo) {
        Tecnico tecnico = resolver(correo);
        return tecnicoRepositoryPort.buscarPorIdBloqueando(tecnico.getId())
                .orElseThrow(() -> new PerfilTecnicoNoEncontradoException(correo));
    }
}
