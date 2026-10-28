package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/** Reúne los datos de perfil de un usuario según su rol. Única razón de cambio: qué incluye cada perfil. */
@Component
public class ConstructorPerfilPropio {

    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public ConstructorPerfilPropio(ClienteRepositoryPort clienteRepositoryPort,
                                   TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    public PerfilPropio construir(Usuario usuario) {
        return switch (usuario.getRol()) {
            case CLIENTE -> clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                    .map(cliente -> deCliente(usuario, cliente))
                    .orElseGet(() -> soloUsuario(usuario));
            case TECNICO -> tecnicoRepositoryPort.buscarPorUsuarioId(usuario.getId())
                    .map(tecnico -> deTecnico(usuario, tecnico))
                    .orElseGet(() -> soloUsuario(usuario));
            default -> soloUsuario(usuario);
        };
    }

    private PerfilPropio deCliente(Usuario usuario, Cliente cliente) {
        return new PerfilPropio(usuario, cliente.getDireccion(), null, List.of());
    }

    private PerfilPropio deTecnico(Usuario usuario, Tecnico tecnico) {
        List<Especialidad> especialidades = tecnico.getEspecialidades() == null
                ? List.of() : List.copyOf(tecnico.getEspecialidades());
        return new PerfilPropio(usuario, tecnico.getDireccionBase(), tecnico.getRadioCoberturaKm(), especialidades);
    }

    private PerfilPropio soloUsuario(Usuario usuario) {
        return new PerfilPropio(usuario, null, null, List.of());
    }
}
