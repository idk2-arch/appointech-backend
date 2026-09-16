package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.ports.in.RegistrarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegistrarClienteUseCaseImpl implements RegistrarClienteUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public RegistrarClienteUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                       ClienteRepositoryPort clienteRepositoryPort,
                                       PasswordEncoderPort passwordEncoderPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public Cliente registrar(String nombre, String apellido, String correo, String contrasena, String telefono,
                             TipoDocumento tipoDocumento, String numeroDocumento, String direccion) {
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
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setActivo(true);
        usuario.setCreadoEn(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepositoryPort.guardar(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuarioGuardado);
        cliente.setDireccion(direccion);

        return clienteRepositoryPort.guardar(cliente);
    }
}