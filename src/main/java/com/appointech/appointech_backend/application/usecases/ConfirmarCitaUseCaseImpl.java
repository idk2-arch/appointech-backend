package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Caso de uso: confirmar la cita validando de nuevo la disponibilidad del técnico.
 * <p>Historias de usuario:
 * <ul>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-14">SCRUM-14</a> - RF04 - Confirmar cita en una franja sugerida</li>
 * </ul>
 */
@Service
public class ConfirmarCitaUseCaseImpl implements ConfirmarCitaUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort;
    private final ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort;
    private final RegistroLogPort registroLogPort;
    private final NotificadorCitaPort notificadorCitaPort;

    public ConfirmarCitaUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                    ClienteRepositoryPort clienteRepositoryPort,
                                    CitaRepositoryPort citaRepositoryPort,
                                    TecnicoRepositoryPort tecnicoRepositoryPort,
                                    BloqueIndisponibilidadRepositoryPort bloqueIndisponibilidadRepositoryPort,
                                    ConfiguracionFranjasRepositoryPort configuracionFranjasRepositoryPort,
                                    RegistroLogPort registroLogPort,
                                    NotificadorCitaPort notificadorCitaPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.citaRepositoryPort = citaRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.bloqueIndisponibilidadRepositoryPort = bloqueIndisponibilidadRepositoryPort;
        this.configuracionFranjasRepositoryPort = configuracionFranjasRepositoryPort;
        this.registroLogPort = registroLogPort;
        this.notificadorCitaPort = notificadorCitaPort;
    }

    @Override
    @Transactional
    public Cita confirmar(String correoCliente, Long solicitudId, Long tecnicoId,
                          LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new FranjaInvalidaException("La hora de fin debe ser posterior a la de inicio");
        }
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new FranjaInvalidaException("La franja debe estar en el futuro");
        }

        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correoCliente)
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));
        Cliente cliente = clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));

        Cita solicitud = citaRepositoryPort.buscarPorId(solicitudId)
                .filter(cita -> cita.getClienteId().equals(cliente.getId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(solicitudId));

        if (solicitud.getEstado() != EstadoCita.PENDIENTE) {
            throw new FranjaNoDisponibleException("La solicitud ya fue confirmada o cancelada");
        }

        validarContraConfiguracion(inicio, fin);

        Tecnico tecnico = tecnicoRepositoryPort.buscarPorIdBloqueando(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));

        boolean atiendeEspecialidad = tecnico.getEspecialidades() != null
                && tecnico.getEspecialidades().stream()
                .map(Especialidad::getId)
                .anyMatch(id -> id.equals(solicitud.getEspecialidadId()));
        if (!atiendeEspecialidad || !tecnico.getUsuario().isActivo()) {
            throw new FranjaNoDisponibleException("El técnico seleccionado no puede atender esta solicitud");
        }

        List<Cita> citasQueCruzan = citaRepositoryPort.buscarActivasDelTecnicoEntre(tecnicoId, inicio, fin);
        List<BloqueIndisponibilidad> bloquesQueCruzan =
                bloqueIndisponibilidadRepositoryPort.buscarDelTecnicoEntre(tecnicoId, inicio, fin);
        boolean ocupado = citasQueCruzan.stream().anyMatch(cita -> !cita.getId().equals(solicitud.getId())
                && cita.getFechaHoraInicio().isBefore(fin) && cita.getFechaHoraFin().isAfter(inicio))
                || bloquesQueCruzan.stream().anyMatch(bloque ->
                bloque.getInicio().isBefore(fin) && bloque.getFin().isAfter(inicio));
        if (ocupado) {
            throw new FranjaNoDisponibleException("La franja seleccionada ya no está disponible");
        }

        solicitud.setTecnicoId(tecnicoId);
        solicitud.setFechaHoraInicio(inicio);
        solicitud.setFechaHoraFin(fin);
        solicitud.setEstado(EstadoCita.CONFIRMADA);
        solicitud.setActualizadaEn(LocalDateTime.now());

        Cita citaConfirmada = citaRepositoryPort.guardar(solicitud);
        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "ConfirmarCitaUseCaseImpl", correoCliente,
                "Cita " + citaConfirmada.getId() + " confirmada con el técnico " + tecnicoId);
        notificadorCitaPort.notificarCitaConfirmada(citaConfirmada, correoCliente);
        return citaConfirmada;
    }

    private void validarContraConfiguracion(LocalDateTime inicio, LocalDateTime fin) {
        ConfiguracionFranjas configuracion = configuracionFranjasRepositoryPort.obtenerActual()
                .orElseGet(() -> new ConfiguracionFranjas(null, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        boolean mismoDia = inicio.toLocalDate().equals(fin.toLocalDate());
        boolean dentroDeJornada = !inicio.toLocalTime().isBefore(configuracion.getHoraInicioJornada())
                && !fin.toLocalTime().isAfter(configuracion.getHoraFinJornada());
        boolean duracionCorrecta = Duration.between(inicio, fin).toMinutes() == configuracion.getDuracionBloqueMinutos();

        if (!mismoDia || !dentroDeJornada || !duracionCorrecta) {
            throw new FranjaInvalidaException("La franja no corresponde a un horario ofrecido");
        }
    }
}
