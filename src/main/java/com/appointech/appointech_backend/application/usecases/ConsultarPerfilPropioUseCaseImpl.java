package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ConstructorPerfilPropio;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPerfilPropioUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ConsultarPerfilPropioUseCaseImpl implements ConsultarPerfilPropioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ConstructorPerfilPropio constructorPerfil;

    public ConsultarPerfilPropioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                            ConstructorPerfilPropio constructorPerfil) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.constructorPerfil = constructorPerfil;
    }

    @Override
    public PerfilPropio consultar(String correoUsuario) {
        return constructorPerfil.construir(usuarioRepositoryPort.buscarPorCorreo(correoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correoUsuario)));
    }
}
