package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.ports.in.AutenticarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TokenGeneratorPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUsuarioUseCaseImpl implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    public AutenticarUsuarioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                        PasswordEncoderPort passwordEncoderPort,
                                        TokenGeneratorPort tokenGeneratorPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
    }

    @Override
    public String autenticar(String correo, String contrasena) {
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (!passwordEncoderPort.coincide(contrasena, usuario.getContrasena())) {
            throw new CredencialesInvalidasException();
        }

        return tokenGeneratorPort.generarToken(usuario);
    }
}