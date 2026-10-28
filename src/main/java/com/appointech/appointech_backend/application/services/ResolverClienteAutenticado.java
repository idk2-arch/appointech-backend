package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

/** Traduce el correo del JWT al cliente que hace la petición. */
@Component
public class ResolverClienteAutenticado {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;

    public ResolverClienteAutenticado(UsuarioRepositoryPort usuarioRepositoryPort,
                                      ClienteRepositoryPort clienteRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
    }

    public Cliente resolver(String correo) {
        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException(correo));
        return clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(correo));
    }
}
