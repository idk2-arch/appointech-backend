package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Decide quién puede tocar una cita según su pertenencia, no solo según su rol: el cliente dueño, el técnico
 * asignado y el administrador. Una cita inexistente es siempre 404.
 */
@Component
public class VerificadorAccesoCita {

    private final CitaRepositoryPort citaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public VerificadorAccesoCita(CitaRepositoryPort citaRepositoryPort, UsuarioRepositoryPort usuarioRepositoryPort,
                                 ClienteRepositoryPort clienteRepositoryPort, TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    /** Devuelve la cita si el correo es el del cliente dueño. */
    public Cita exigirDuenoCliente(String correo, Long citaId) {
        Cita cita = buscarCita(citaId);
        if (!esClienteDuenio(buscarUsuario(correo), cita)) {
            throw new AccesoCitaDenegadoException();
        }
        return cita;
    }

    /** Devuelve la cita si el correo es el del cliente dueño, el técnico asignado o un administrador. */
    public Cita exigirAcceso(String correo, Long citaId) {
        Cita cita = buscarCita(citaId);
        if (!tieneAcceso(buscarUsuario(correo), cita)) {
            throw new AccesoCitaDenegadoException();
        }
        return cita;
    }

    private boolean tieneAcceso(Usuario usuario, Cita cita) {
        return usuario.getRol() == RolUsuario.ADMINISTRADOR
                || esClienteDuenio(usuario, cita)
                || esTecnicoAsignado(usuario, cita);
    }

    private boolean esClienteDuenio(Usuario usuario, Cita cita) {
        return usuario.getRol() == RolUsuario.CLIENTE
                && clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .map(cliente -> Objects.equals(cliente.getId(), cita.getClienteId()))
                .orElse(false);
    }

    private boolean esTecnicoAsignado(Usuario usuario, Cita cita) {
        return usuario.getRol() == RolUsuario.TECNICO
                && cita.getTecnicoId() != null
                && tecnicoRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .map(tecnico -> Objects.equals(tecnico.getId(), cita.getTecnicoId()))
                .orElse(false);
    }

    private Cita buscarCita(Long citaId) {
        return citaRepositoryPort.buscarPorId(citaId).orElseThrow(() -> new SolicitudNoEncontradaException(citaId));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));
    }
}
