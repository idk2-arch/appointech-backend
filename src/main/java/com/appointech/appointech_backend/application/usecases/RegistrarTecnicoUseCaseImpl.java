package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegistrarTecnicoUseCaseImpl implements RegistrarTecnicoUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public RegistrarTecnicoUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                       TecnicoRepositoryPort tecnicoRepositoryPort,
                                       PasswordEncoderPort passwordEncoderPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public Tecnico registrar(String nombre, String apellido, String correo, String contrasena, String telefono,
                             TipoDocumento tipoDocumento, String numeroDocumento, String direccionBase,
                             Integer radioCoberturaKm) {
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
        usuario.setRol(RolUsuario.TECNICO);
        usuario.setActivo(true);
        usuario.setCreadoEn(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepositoryPort.guardar(usuario);

        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuarioGuardado);
        tecnico.setDireccionBase(direccionBase);
        tecnico.setRadioCoberturaKm(radioCoberturaKm != null ? radioCoberturaKm : 10);

        return tecnicoRepositoryPort.guardar(tecnico);
    }
}