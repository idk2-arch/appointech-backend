package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.ports.in.CrearAdministradorUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CrearAdministradorUseCaseImpl implements CrearAdministradorUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public CrearAdministradorUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                         PasswordEncoderPort passwordEncoderPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public Usuario crear(String nombre, String apellido, String correo, String contrasena, String telefono,
                         TipoDocumento tipoDocumento, String numeroDocumento) {
        if (usuarioRepositoryPort.buscarPorCorreo(correo).isPresent()) {
            throw new CorreoYaRegistradoException(correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setCorreo(correo);
        usuario.setContrasena(passwordEncoderPort.encriptar(contrasena));
        usuario.setTelefono(telefono);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNumeroDocumento(numeroDocumento);
        usuario.setRol(RolUsuario.ADMINISTRADOR);
        usuario.setActivo(true);
        usuario.setCreadoEn(LocalDateTime.now());

        return usuarioRepositoryPort.guardar(usuario);
    }
}