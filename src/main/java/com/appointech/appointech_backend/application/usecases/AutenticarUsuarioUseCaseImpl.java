package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.PerfilUtil;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.ports.in.AutenticarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TokenGeneratorPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUsuarioUseCaseImpl implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    public AutenticarUsuarioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                        ClienteRepositoryPort clienteRepositoryPort,
                                        PasswordEncoderPort passwordEncoderPort,
                                        TokenGeneratorPort tokenGeneratorPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
    }

    @Override
    public ResultadoAutenticacion autenticar(String correo, String contrasena) {
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (usuario.getContrasena() == null || !passwordEncoderPort.coincide(contrasena, usuario.getContrasena())) {
            throw new CredencialesInvalidasException();
        }

        String token = tokenGeneratorPort.generarToken(usuario);
        boolean perfilCompleto = calcularPerfilCompleto(usuario);

        return new ResultadoAutenticacion(token, perfilCompleto);
    }

    private boolean calcularPerfilCompleto(Usuario usuario) {
        Cliente cliente = clienteRepositoryPort.buscarPorUsuarioId(usuario.getId()).orElse(null);
        return PerfilUtil.estaCompleto(usuario, cliente);
    }
}