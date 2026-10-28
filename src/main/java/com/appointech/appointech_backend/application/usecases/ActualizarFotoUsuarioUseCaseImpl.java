package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorFotoUsuario;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

/** Caso de uso: el administrador guarda o reemplaza la foto de perfil de otro usuario. */
@Service
public class ActualizarFotoUsuarioUseCaseImpl implements ActualizarFotoUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final GestorFotoUsuario gestorFotoUsuario;

    public ActualizarFotoUsuarioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                            GestorFotoUsuario gestorFotoUsuario) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.gestorFotoUsuario = gestorFotoUsuario;
    }

    @Override
    public String reemplazar(Long usuarioId, String imagenBase64) {
        return gestorFotoUsuario.reemplazar(
                usuarioRepositoryPort.buscarPorId(usuarioId)
                        .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId)),
                imagenBase64);
    }
}
