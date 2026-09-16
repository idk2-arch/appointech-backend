package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ActualizarUsuarioUseCaseImpl implements ActualizarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ActualizarUsuarioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public Usuario actualizar(Long id, String nombre, String apellido, String telefono) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setTelefono(telefono);

        return usuarioRepositoryPort.guardar(usuario);
    }
}