package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.CambiarEstadoUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CambiarEstadoUsuarioUseCaseImpl implements CambiarEstadoUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public CambiarEstadoUsuarioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.setActivo(activo);

        return usuarioRepositoryPort.guardar(usuario);
    }
}