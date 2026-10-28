package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorFotoUsuario;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoPerfilUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

/** Caso de uso: cualquier usuario autenticado guarda, reemplaza o borra su propia foto de perfil. */
@Service
public class ActualizarFotoPerfilUseCaseImpl implements ActualizarFotoPerfilUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final GestorFotoUsuario gestorFotoUsuario;

    public ActualizarFotoPerfilUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                           GestorFotoUsuario gestorFotoUsuario) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.gestorFotoUsuario = gestorFotoUsuario;
    }

    @Override
    public String reemplazar(String correo, String imagenBase64) {
        return gestorFotoUsuario.reemplazar(buscar(correo), imagenBase64);
    }

    @Override
    public void eliminar(String correo) {
        gestorFotoUsuario.eliminar(buscar(correo));
    }

    private Usuario buscar(String correo) {
        return usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));
    }
}
