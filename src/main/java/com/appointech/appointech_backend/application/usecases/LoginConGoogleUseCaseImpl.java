package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.PerfilUtil;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort.GoogleUser;
import com.appointech.appointech_backend.domain.ports.out.TokenGeneratorPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Caso de uso: iniciar sesión con Google.
 *
 * 1. Verifica el ID token con Google (a través del puerto de salida).
 * 2. Busca el usuario por correo:
 *    - Si existe (Cliente, Técnico o Administrador), entra con su rol actual.
 *    - Si no existe, se registra como CLIENTE (con su fila en la tabla cliente)
 *      (técnicos y administradores los crea el administrador).
 *    - Si está inactivo, se rechaza igual que en el login normal.
 * 3. Devuelve el mismo JWT propio que el login normal, más si el perfil está completo.
 */
@Service
public class LoginConGoogleUseCaseImpl implements LoginConGoogleUseCase {

    private final GoogleTokenVerifierPort googleVerifier;
    private final UsuarioRepositoryPort usuarioRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final TokenGeneratorPort tokenGenerator;

    public LoginConGoogleUseCaseImpl(GoogleTokenVerifierPort googleVerifier,
                                     UsuarioRepositoryPort usuarioRepository,
                                     ClienteRepositoryPort clienteRepository,
                                     TokenGeneratorPort tokenGenerator) {
        this.googleVerifier = googleVerifier;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public ResultadoLogin ejecutar(String idToken) {
        GoogleUser googleUser = googleVerifier.verificar(idToken)
                .orElseThrow(CredencialesInvalidasException::new);

        Usuario usuario = usuarioRepository.buscarPorCorreo(googleUser.correo())
                .orElseGet(() -> registrarClienteNuevo(googleUser));

        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }

        String jwt = tokenGenerator.generarToken(usuario);

        Cliente cliente = clienteRepository.buscarPorUsuarioId(usuario.getId()).orElse(null);
        boolean perfilCompleto = PerfilUtil.estaCompleto(usuario, cliente);

        return new ResultadoLogin(
                jwt,
                usuario.getRol().name(),
                usuario.getNombre(),
                usuario.getCorreo(),
                perfilCompleto
        );
    }

    /**
     * Registra un cliente nuevo con los datos de Google, incluyendo su fila
     * en la tabla cliente (sin dirección todavía — se completa después con
     * PATCH /api/auth/completar-perfil).
     * Queda SIN contraseña: solo puede entrar con Google
     * (la columna "contrasena" debe permitir null).
     */
    private Usuario registrarClienteNuevo(GoogleUser googleUser) {
        Usuario nuevo = new Usuario();
        nuevo.setNombre(googleUser.nombre());
        nuevo.setApellido(googleUser.apellido());
        nuevo.setCorreo(googleUser.correo());
        nuevo.setContrasena(null);
        nuevo.setRol(RolUsuario.CLIENTE);
        nuevo.setActivo(true);
        nuevo.setCreadoEn(LocalDateTime.now());
        Usuario usuarioGuardado = usuarioRepository.guardar(nuevo);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuarioGuardado);
        clienteRepository.guardar(cliente);

        return usuarioGuardado;
    }
}