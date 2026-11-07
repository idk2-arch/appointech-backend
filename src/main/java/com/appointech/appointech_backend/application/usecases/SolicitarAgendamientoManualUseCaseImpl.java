package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.SolicitudManualNoPermitidaException;
import com.appointech.appointech_backend.domain.exception.TelefonoContactoRequeridoException;
import com.appointech.appointech_backend.application.services.AvisosInternos;
import com.appointech.appointech_backend.application.services.GestorCotizacion;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.SolicitarAgendamientoManualUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el cliente pide que un administrador le agende la cita a mano porque su dirección
 * queda fuera de la cobertura de todos los técnicos de la especialidad.
 */
@Service
public class SolicitarAgendamientoManualUseCaseImpl implements SolicitarAgendamientoManualUseCase {

    static final int DIAS_MAXIMOS_HACIA_ADELANTE = 90;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final CalcularFranjasDisponiblesUseCase calcularFranjasDisponiblesUseCase;
    private final RegistroLogPort registroLogPort;
    private final GestorCotizacion gestorCotizacion;
    private final AvisosInternos avisos;
    private final Clock reloj;

    public SolicitarAgendamientoManualUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                                  ClienteRepositoryPort clienteRepositoryPort,
                                                  EspecialidadRepositoryPort especialidadRepositoryPort,
                                                  CitaRepositoryPort citaRepositoryPort,
                                                  CalcularFranjasDisponiblesUseCase calcularFranjasDisponiblesUseCase,
                                                  RegistroLogPort registroLogPort,
                                                  GestorCotizacion gestorCotizacion,
                                                  AvisosInternos avisos, Clock reloj) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
        this.citaRepositoryPort = citaRepositoryPort;
        this.calcularFranjasDisponiblesUseCase = calcularFranjasDisponiblesUseCase;
        this.registroLogPort = registroLogPort;
        this.gestorCotizacion = gestorCotizacion;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Cita solicitar(String correoCliente, Long especialidadId, String descripcionProblema,
                          String direccionServicio, Double latitud, Double longitud, String telefonoContacto,
                          LocalDate fecha) {
        Coordenadas ubicacion = Coordenadas.de(latitud, longitud);
        LocalDate diaConsultado = diaConsultado(fecha);

        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correoCliente)
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));
        Cliente cliente = clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));

        Especialidad especialidad = especialidadRepositoryPort.buscarPorId(especialidadId)
                .filter(Especialidad::isActivo)
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(especialidadId)));

        String telefono = elegirTelefono(telefonoContacto, usuario.getTelefono());

        ResultadoFranjas resultado = calcularFranjasDisponiblesUseCase.calcularConMotivo(
                especialidadId, diaConsultado, ubicacion.latitud(), ubicacion.longitud());
        MotivoSolicitudEspecial motivoEspecial = motivoEspecialDe(resultado.motivo());
        if (motivoEspecial == null) {
            throw new SolicitudManualNoPermitidaException(mensajeNoPermitida(resultado.motivo()));
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Cita solicitud = new Cita();
        solicitud.setClienteId(cliente.getId());
        solicitud.setEspecialidadId(especialidadId);
        solicitud.setDescripcionProblema(descripcionProblema);
        solicitud.setDireccionServicio(direccionServicio);
        solicitud.setLatitud(ubicacion.latitud());
        solicitud.setLongitud(ubicacion.longitud());
        solicitud.setEstado(EstadoCita.PENDIENTE);
        solicitud.setAgendamientoManual(true);
        solicitud.setFechaDeseada(fecha);
        solicitud.setMotivoSolicitudEspecial(motivoEspecial);
        solicitud.setTelefonoContacto(telefono);
        solicitud.setCreadaEn(ahora);
        solicitud.setActualizadaEn(ahora);

        Cita guardada = citaRepositoryPort.guardar(solicitud);
        gestorCotizacion.registrar(guardada.getId(), especialidad);
        registroLogPort.registrar(NivelLog.INFO, TipoLog.NEGOCIO, "SolicitarAgendamientoManualUseCaseImpl", correoCliente,
                "Solicitud de agendamiento manual creada con id " + guardada.getId());
        avisos.solicitudRecibida(guardada);
        return guardada;
    }

    /** Si el cliente indica un teléfono de contacto se usa ese; si no, el de su cuenta. */
    private String elegirTelefono(String indicado, String delUsuario) {
        if (indicado != null && !indicado.isBlank()) {
            return indicado.trim();
        }
        if (delUsuario != null && !delUsuario.isBlank()) {
            return delUsuario;
        }
        throw new TelefonoContactoRequeridoException();
    }

    /** El día cuya disponibilidad se revisa: el que el cliente consultaba (hoy si no lo indica); no puede ser pasado ni superar 90 días. */
    private LocalDate diaConsultado(LocalDate fecha) {
        LocalDate hoy = LocalDate.now(reloj);
        if (fecha == null) {
            return hoy;
        }
        if (fecha.isBefore(hoy)) {
            throw new FranjaInvalidaException("La fecha no puede ser anterior a hoy");
        }
        if (fecha.isAfter(hoy.plusDays(DIAS_MAXIMOS_HACIA_ADELANTE))) {
            throw new FranjaInvalidaException("La fecha no puede superar " + DIAS_MAXIMOS_HACIA_ADELANTE + " días desde hoy");
        }
        return fecha;
    }

    /** Fuera de cobertura y sin horarios habilitan la solicitud manual; sin técnicos o con franjas libres, no (null). */
    private static MotivoSolicitudEspecial motivoEspecialDe(MotivoSinFranjas motivo) {
        if (motivo == MotivoSinFranjas.FUERA_DE_COBERTURA) {
            return MotivoSolicitudEspecial.FUERA_DE_COBERTURA;
        }
        return motivo == MotivoSinFranjas.SIN_HORARIOS ? MotivoSolicitudEspecial.SIN_HORARIOS : null;
    }

    private String mensajeNoPermitida(MotivoSinFranjas motivo) {
        if (motivo == MotivoSinFranjas.SIN_TECNICOS_ESPECIALIDAD) {
            return "No hay técnicos disponibles para esta especialidad; el agendamiento manual solo aplica "
                    + "cuando la dirección está fuera de cobertura";
        }
        return "La dirección está dentro de la cobertura; elige una de las franjas disponibles. "
                + "El agendamiento manual solo aplica cuando la dirección está fuera de cobertura o no hay horarios disponibles";
    }
}
