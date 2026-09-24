package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.CompletarPerfilUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CompletarPerfilUseCaseImpl implements CompletarPerfilUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;

    public CompletarPerfilUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                      ClienteRepositoryPort clienteRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
    }

    @Override
    public Usuario completar(String correoUsuario, String telefono, TipoDocumento tipoDocumento,
                             String numeroDocumento, String direccion) {

        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(-1L));

        usuario.setTelefono(telefono);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNumeroDocumento(numeroDocumento);
        Usuario usuarioActualizado = usuarioRepositoryPort.guardar(usuario);

        if (usuario.getRol() == RolUsuario.CLIENTE && direccion != null && !direccion.isBlank()) {
            clienteRepositoryPort.buscarPorUsuarioId(usuario.getId()).ifPresent(cliente -> {
                cliente.setDireccion(direccion);
                clienteRepositoryPort.guardar(cliente);
            });
        }

        return usuarioActualizado;
    }
}