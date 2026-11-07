package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.DestinatariosAdminPort;
import com.appointech.appointech_backend.domain.ports.out.PublicadorNotificacionesPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Crea las notificaciones internas (la campana) de cada evento. Se invoca después de guardar el cambio principal y
 * nunca lo afecta: cualquier fallo aquí se registra en el log y el caso de uso sigue. Los textos no llevan correos,
 * teléfonos ni datos de otras personas.
 */
@Component
public class AvisosInternos {

    private static final Logger log = LoggerFactory.getLogger(AvisosInternos.class);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TITULO_CANCELADA = "Cita cancelada";

    private final PublicadorNotificacionesPort publicador;
    private final DestinatariosAdminPort destinatariosAdmin;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final Clock reloj;

    public AvisosInternos(PublicadorNotificacionesPort publicador, DestinatariosAdminPort destinatariosAdmin,
                          ClienteRepositoryPort clienteRepositoryPort, TecnicoRepositoryPort tecnicoRepositoryPort,
                          CitaRepositoryPort citaRepositoryPort, Clock reloj) {
        this.publicador = publicador;
        this.destinatariosAdmin = destinatariosAdmin;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.citaRepositoryPort = citaRepositoryPort;
        this.reloj = reloj;
    }

    /** Cliente: solicitud recibida. Si quedó para agendamiento manual, además avisa a los administradores. */
    public void solicitudRecibida(Cita solicitud) {
        boolean manual = solicitud.getMotivoSolicitudEspecial() != null;
        String siguiente = manual ? "Un administrador coordinará tu cita."
                : "Elige una franja disponible para confirmarla.";
        alCliente(solicitud, TipoNotificacion.SOLICITUD_RECIBIDA, "Solicitud recibida",
                "Recibimos tu solicitud #" + solicitud.getId() + ". " + siguiente);
        if (manual) {
            alosAdministradores(solicitud);
        }
    }

    /** Cliente: cita confirmada. Técnico: nueva cita asignada. */
    public void citaConfirmada(Cita cita) {
        String cuando = cita.getFechaHoraInicio() == null ? "" : " para el " + cita.getFechaHoraInicio().format(FECHA_HORA);
        alCliente(cita, TipoNotificacion.CITA_CONFIRMADA, "Cita confirmada",
                "Tu cita #" + cita.getId() + " quedó confirmada" + cuando + ".");
        alTecnico(cita, TipoNotificacion.NUEVA_CITA_ASIGNADA, "Nueva cita asignada",
                "Se te asignó la cita #" + cita.getId() + cuando + ".");
    }

    /** El cliente canceló: se avisa a él y al técnico asignado. */
    public void citaCanceladaPorCliente(Cita cita) {
        alCliente(cita, TipoNotificacion.CITA_CANCELADA, TITULO_CANCELADA, "Tu cita #" + cita.getId() + " fue cancelada.");
        alTecnico(cita, TipoNotificacion.CITA_CANCELADA, TITULO_CANCELADA,
                "El cliente canceló la cita #" + cita.getId() + ".");
    }

    /** El técnico canceló: se avisa al cliente. */
    public void citaCanceladaPorTecnico(Cita cita) {
        alCliente(cita, TipoNotificacion.CITA_CANCELADA, TITULO_CANCELADA,
                "El técnico canceló tu cita #" + cita.getId() + ".");
    }

    public void citaFinalizada(Cita cita) {
        alCliente(cita, TipoNotificacion.CITA_FINALIZADA, "Servicio finalizado",
                "Tu cita #" + cita.getId() + " finalizó. Ya puedes calificar el servicio.");
    }

    /** Cliente: pago registrado (se llama una sola vez, cuando el pago queda PAGADO). */
    public void pagoRegistrado(Pago pago) {
        try {
            citaRepositoryPort.buscarPorId(pago.getCitaId()).ifPresent(cita -> alCliente(cita,
                    TipoNotificacion.PAGO_REGISTRADO, "Pago registrado",
                    "Registramos el pago de tu cita #" + cita.getId() + "."));
        } catch (RuntimeException ex) {
            registrarFallo(pago.getCitaId(), ex);
        }
    }

    private void alCliente(Cita cita, TipoNotificacion tipo, String titulo, String mensaje) {
        enviar(cita, tipo, titulo, mensaje, () -> clienteRepositoryPort.buscarPorId(cita.getClienteId())
                .map(cliente -> cliente.getUsuario().getId()));
    }

    private void alTecnico(Cita cita, TipoNotificacion tipo, String titulo, String mensaje) {
        if (cita.getTecnicoId() == null) {
            return;
        }
        enviar(cita, tipo, titulo, mensaje, () -> tecnicoRepositoryPort.buscarPorId(cita.getTecnicoId())
                .map(tecnico -> tecnico.getUsuario().getId()));
    }

    private void alosAdministradores(Cita solicitud) {
        boolean fueraDeCobertura = solicitud.getMotivoSolicitudEspecial() == MotivoSolicitudEspecial.FUERA_DE_COBERTURA;
        TipoNotificacion tipo = fueraDeCobertura ? TipoNotificacion.SOLICITUD_FUERA_DE_COBERTURA
                : TipoNotificacion.SOLICITUD_SIN_TECNICOS;
        String titulo = fueraDeCobertura ? "Solicitud fuera de cobertura" : "Solicitud sin técnicos";
        String mensaje = "La solicitud #" + solicitud.getId() + (fueraDeCobertura
                ? " está fuera de cobertura y necesita agendamiento manual."
                : " no tiene técnicos de la especialidad y necesita agendamiento manual.");
        try {
            for (Long administradorId : destinatariosAdmin.idsAdministradoresActivos()) {
                publicador.publicar(Notificacion.nueva(administradorId, tipo, titulo, mensaje, solicitud.getId(), ahora()));
            }
        } catch (RuntimeException ex) {
            registrarFallo(solicitud.getId(), ex);
        }
    }

    private void enviar(Cita cita, TipoNotificacion tipo, String titulo, String mensaje,
                        Supplier<Optional<Long>> buscarUsuarioId) {
        try {
            buscarUsuarioId.get().ifPresent(usuarioId ->
                    publicador.publicar(Notificacion.nueva(usuarioId, tipo, titulo, mensaje, cita.getId(), ahora())));
        } catch (RuntimeException ex) {
            registrarFallo(cita.getId(), ex);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private void registrarFallo(Long citaId, RuntimeException ex) {
        log.warn("No se pudo crear la notificación interna de la cita {}: {}", citaId, ex.getClass().getSimpleName());
    }
}
