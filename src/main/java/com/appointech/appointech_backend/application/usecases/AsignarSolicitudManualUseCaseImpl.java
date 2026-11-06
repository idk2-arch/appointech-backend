package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasAsignacionManual;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el administrador asigna técnico y horario a una solicitud de agendamiento manual (RF15).
 * No exige jornada, duración ni cobertura (el administrador decide), pero sí técnico existente y activo, con la
 * especialidad de la solicitud y sin citas ni bloqueos que se crucen. Deja registrado quién asignó y cuándo.
 */
@Service
public class AsignarSolicitudManualUseCaseImpl implements AsignarSolicitudManualUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final ReglasAsignacionManual reglasAsignacionManual;
    private final RegistroLogPort registroLogPort;
    private final NotificadorCitaPort notificadorCitaPort;
    private final Clock reloj;

    public AsignarSolicitudManualUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                             TecnicoRepositoryPort tecnicoRepositoryPort,
                                             UsuarioRepositoryPort usuarioRepositoryPort,
                                             ClienteRepositoryPort clienteRepositoryPort,
                                             ReglasAsignacionManual reglasAsignacionManual,
                                             RegistroLogPort registroLogPort,
                                             NotificadorCitaPort notificadorCitaPort,
                                             Clock reloj) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.reglasAsignacionManual = reglasAsignacionManual;
        this.registroLogPort = registroLogPort;
        this.notificadorCitaPort = notificadorCitaPort;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Cita asignar(String correoAdministrador, Long solicitudId, Long tecnicoId,
                        LocalDateTime inicio, LocalDateTime fin) {
        reglasAsignacionManual.exigirRangoValido(inicio, fin);
        Cita solicitud = reglasAsignacionManual.solicitudPendiente(solicitudId);
        Usuario administrador = usuarioRepositoryPort.buscarPorCorreo(correoAdministrador)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correoAdministrador));

        Tecnico tecnico = tecnicoRepositoryPort.buscarPorIdBloqueando(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
        boolean atiendeEspecialidad = tecnico.getEspecialidades() != null
                && tecnico.getEspecialidades().stream().map(Especialidad::getId)
                .anyMatch(id -> id.equals(solicitud.getEspecialidadId()));
        if (!atiendeEspecialidad) {
            throw new FranjaNoDisponibleException("El técnico seleccionado no atiende la especialidad de esta solicitud");
        }
        if (!tecnico.getUsuario().isActivo()) {
            throw new FranjaNoDisponibleException("El técnico seleccionado no está activo");
        }
        if (reglasAsignacionManual.tecnicoOcupado(tecnicoId, solicitud.getId(), inicio, fin)) {
            throw new FranjaNoDisponibleException("El técnico tiene una cita o un bloqueo en ese horario");
        }

        solicitud.asignarManualmente(tecnicoId, inicio, fin, administrador.getId(), LocalDateTime.now(reloj));
        Cita confirmada = citaRepositoryPort.guardar(solicitud);

        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "AsignarSolicitudManualUseCaseImpl", correoAdministrador,
                "Solicitud manual " + confirmada.getId() + " asignada al técnico " + tecnicoId);
        clienteRepositoryPort.buscarPorId(confirmada.getClienteId())
                .ifPresent(cliente -> notificadorCitaPort.notificarCitaConfirmada(confirmada, cliente.getUsuario().getCorreo()));
        return confirmada;
    }
}
