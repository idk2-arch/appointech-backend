package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ConstructorPerfilPropio;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.DatosPerfilEditable;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarPerfilPropioUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edita solo nombre, apellido, teléfono y dirección del usuario identificado por el JWT.
 * Correo, documento, rol, estado, especialidades y radio no se tocan aquí (los cambia el administrador).
 */
@Service
public class ActualizarPerfilPropioUseCaseImpl implements ActualizarPerfilPropioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final ConstructorPerfilPropio constructorPerfil;

    public ActualizarPerfilPropioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                             ClienteRepositoryPort clienteRepositoryPort,
                                             TecnicoRepositoryPort tecnicoRepositoryPort,
                                             ConstructorPerfilPropio constructorPerfil) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.constructorPerfil = constructorPerfil;
    }

    @Override
    @Transactional
    public PerfilPropio actualizar(String correoUsuario, DatosPerfilEditable datos) {
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correoUsuario));

        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setTelefono(datos.telefono().trim());
        Usuario guardado = usuarioRepositoryPort.guardar(usuario);

        if (datos.direccion() != null) {
            actualizarDireccion(guardado, datos.direccion().trim());
        }
        return constructorPerfil.construir(guardado);
    }

    private void actualizarDireccion(Usuario usuario, String direccion) {
        switch (usuario.getRol()) {
            case CLIENTE -> clienteRepositoryPort.buscarPorUsuarioId(usuario.getId()).ifPresent(cliente -> {
                cliente.setDireccion(direccion);
                clienteRepositoryPort.guardar(cliente);
            });
            case TECNICO -> tecnicoRepositoryPort.buscarPorUsuarioId(usuario.getId()).ifPresent(tecnico -> {
                tecnico.setDireccionBase(direccion);
                tecnicoRepositoryPort.guardar(tecnico);
            });
            default -> { /* el administrador no tiene dirección */ }
        }
    }
}
