package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el administrador asigna técnico y horario a una solicitud de agendamiento manual.
 * No exige cobertura (el administrador decide), pero sí especialidad, técnico activo y sin choques.
 */
@Service
public class AsignarSolicitudManualUseCaseImpl implements AsignarSolicitudManualUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final RegistroLogPort registroLogPort;
    private final NotificadorCitaPort notificadorCitaPort;

    public AsignarSolicitudManualUseCaseImpl(CitaRepositoryPort citaRepositoryPort,
                                             TecnicoRepositoryPort tecnicoRepositoryPort,
                                             BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
                                             ClienteRepositoryPort clienteRepositoryPort,
                                             RegistroLogPort registroLogPort,
                                             NotificadorCitaPort notificadorCitaPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.registroLogPort = registroLogPort;
        this.notificadorCitaPort = notificadorCitaPort;
    }

    @Override
    @Transactional
    public Cita asignar(String correoAdministrador, Long solicitudId, Long tecnicoId,
                        LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new FranjaInvalidaException("La hora de fin debe ser posterior a la de inicio");
        }

        Cita solicitud = citaRepositoryPort.buscarPorId(solicitudId)
                .filter(Cita::isAgendamientoManual)
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));
        if (solicitud.getEstado() != EstadoCita.PENDIENTE || solicitud.getTecnicoId() != null) {
            throw new FranjaNoDisponibleException("La solicitud ya no está pendiente");
        }

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

        List<Cita> citas = citaRepositoryPort.buscarActivasDelTecnicoEntre(tecnicoId, inicio, fin);
        List<BloqueIndisponibilidad> bloques = bloqueIndisponibilidadRepositoryPort.buscarDelTecnicoEntre(tecnicoId, inicio, fin);
        boolean ocupado = citas.stream().anyMatch(cita -> !cita.getId().equals(solicitud.getId())
                && cita.getFechaHoraInicio().isBefore(fin) && cita.getFechaHoraFin().isAfter(inicio))
                || bloques.stream().anyMatch(bloque -> bloque.getInicio().isBefore(fin) && bloque.getFin().isAfter(inicio));
        if (ocupado) {
            throw new FranjaNoDisponibleException("El técnico tiene una cita o un bloqueo en ese horario");
        }

        solicitud.setTecnicoId(tecnicoId);
        solicitud.setFechaHoraInicio(inicio);
        solicitud.setFechaHoraFin(fin);
        solicitud.setEstado(EstadoCita.CONFIRMADA);
        solicitud.setActualizadaEn(LocalDateTime.now());
        Cita confirmada = citaRepositoryPort.guardar(solicitud);

        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "AsignarSolicitudManualUseCaseImpl", correoAdministrador,
                "Solicitud manual " + confirmada.getId() + " asignada al técnico " + tecnicoId);
        clienteRepositoryPort.buscarPorId(confirmada.getClienteId())
                .ifPresent(cliente -> notificadorCitaPort.notificarCitaConfirmada(confirmada, cliente.getUsuario().getCorreo()));
        return confirmada;
    }
}
