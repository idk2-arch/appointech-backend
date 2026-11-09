package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrarTecnicoUseCaseImpl implements RegistrarTecnicoUseCase {

    private static final int RADIO_POR_DEFECTO_KM = 10;

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
    @Transactional
    public Tecnico registrar(String nombre, String apellido, String correo, String contrasena, String telefono,
                             TipoDocumento tipoDocumento, String numeroDocumento, String direccionBase,
                             Integer radioCoberturaKm, Double latitudBase, Double longitudBase) {
        Coordenadas base = Coordenadas.opcional(latitudBase, longitudBase);

        // Se valida el radio (1-100 km) antes de guardar nada: lanza RadioCoberturaInvalidoException (400).
        Tecnico tecnico = new Tecnico();
        tecnico.cambiarRadioCobertura(radioCoberturaKm != null ? radioCoberturaKm : RADIO_POR_DEFECTO_KM);

        if (usuarioRepositoryPort.buscarPorCorreo(correo).isPresent()) {
            throw new CorreoYaRegistradoException(correo);
        }

        if (numeroDocumento != null && usuarioRepositoryPort.buscarPorNumeroDocumento(numeroDocumento).isPresent()) {
            throw new DocumentoYaRegistradoException(numeroDocumento);
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

        tecnico.setUsuario(usuarioGuardado);
        tecnico.setDireccionBase(direccionBase);
        if (base != null) {
            tecnico.setLatitudBase(base.latitud());
            tecnico.setLongitudBase(base.longitud());
        }

        return tecnicoRepositoryPort.guardar(tecnico);
    }
}