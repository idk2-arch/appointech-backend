package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Lo que comparten los casos de uso que cambian una cita: guardarla con su fila de historial en la misma
 * transacción del llamador y avisar a los participantes sin que un fallo del aviso afecte el cambio.
 */
@Component
public class GestorCambiosCita {

    private static final Logger log = LoggerFactory.getLogger(GestorCambiosCita.class);

    private final CitaRepositoryPort citaRepositoryPort;
    private final HistorialEstadoCitaPort historialPort;
    private final NotificadorCitaPort notificadorCitaPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final RegistroLogPort registroLogPort;
    private final Clock reloj;

    public GestorCambiosCita(CitaRepositoryPort citaRepositoryPort,
                             HistorialEstadoCitaPort historialPort,
                             NotificadorCitaPort notificadorCitaPort,
                             ClienteRepositoryPort clienteRepositoryPort,
                             TecnicoRepositoryPort tecnicoRepositoryPort,
                             RegistroLogPort registroLogPort,
                             Clock reloj) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.historialPort = historialPort;
        this.notificadorCitaPort = notificadorCitaPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.registroLogPort = registroLogPort;
        this.reloj = reloj;
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    /** Guarda la cita sin tocar el historial (p. ej. al registrar el diagnóstico). */
    public Cita guardar(Cita cita) {
        cita.setActualizadaEn(ahora());
        return citaRepositoryPort.guardar(cita);
    }

    /** Guarda la cita e inserta la fila de historial; el llamador debe estar dentro de una transacción. */
    public Cita guardarConHistorial(Cita cita, EstadoCita estadoAnterior, String actor, String motivo) {
        Cita guardada = guardar(cita);
        historialPort.registrar(new HistorialEstadoCita(null, guardada.getId(), estadoAnterior,
                guardada.getEstado(), guardada.getActualizadaEn(), actor, motivo));
        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "GestorCambiosCita", actor,
                "Cita " + guardada.getId() + ": " + estadoAnterior + " -> " + guardada.getEstado());
        return guardada;
    }

    public void notificarAlCliente(Cita cita, String mensaje) {
        notificar(cita, mensaje, () -> clienteRepositoryPort.buscarPorId(cita.getClienteId())
                .map(cliente -> cliente.getUsuario().getCorreo()));
    }

    public void notificarAlTecnico(Cita cita, String mensaje) {
        if (cita.getTecnicoId() == null) {
            return;
        }
        notificar(cita, mensaje, () -> tecnicoRepositoryPort.buscarPorId(cita.getTecnicoId())
                .map(tecnico -> tecnico.getUsuario().getCorreo()));
    }

    private void notificar(Cita cita, String mensaje, Supplier<Optional<String>> buscarCorreo) {
        try {
            buscarCorreo.get().ifPresent(destino -> notificadorCitaPort.notificarCambioDeCita(cita, destino, mensaje));
        } catch (RuntimeException ex) {
            // El cambio ya quedó guardado: un aviso fallido nunca lo revierte ni lo bloquea.
            log.warn("No se pudo enviar la notificación de la cita {}: {}", cita.getId(), ex.getClass().getSimpleName());
        }
    }
}
