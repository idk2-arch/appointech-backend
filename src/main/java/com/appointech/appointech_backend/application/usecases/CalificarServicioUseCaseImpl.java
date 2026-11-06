package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.CitaNoFinalizadaException;
import com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException;
import com.appointech.appointech_backend.domain.exception.PlazoCalificacionVencidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ConfiguracionCalificacion;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.ports.in.CalificarServicioUseCase;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caso de uso (RF07): el cliente dueño califica su cita finalizada, una sola vez y dentro del plazo configurado.
 * No exige que el pago esté realizado.
 */
@Service
public class CalificarServicioUseCaseImpl implements CalificarServicioUseCase {

    private final ResolverClienteAutenticado resolverCliente;
    private final CitaRepositoryPort citaRepositoryPort;
    private final CalificacionRepositoryPort calificacionRepositoryPort;
    private final HistorialEstadoCitaPort historialPort;
    private final ConfiguracionCalificacion configuracion;
    private final Clock reloj;

    public CalificarServicioUseCaseImpl(ResolverClienteAutenticado resolverCliente,
                                        CitaRepositoryPort citaRepositoryPort,
                                        CalificacionRepositoryPort calificacionRepositoryPort,
                                        HistorialEstadoCitaPort historialPort,
                                        ConfiguracionCalificacion configuracion,
                                        Clock reloj) {
        this.resolverCliente = resolverCliente;
        this.citaRepositoryPort = citaRepositoryPort;
        this.calificacionRepositoryPort = calificacionRepositoryPort;
        this.historialPort = historialPort;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Calificacion calificar(String correoCliente, Long citaId, Integer puntuacion, String comentario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Calificacion nueva = Calificacion.nueva(citaId, puntuacion, comentario, ahora);

        Cliente cliente = resolverCliente.resolver(correoCliente);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> cliente.getId().equals(c.getClienteId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));
        if (cita.getEstado() != EstadoCita.FINALIZADA) {
            throw new CitaNoFinalizadaException(cita.getEstado().name());
        }
        if (calificacionRepositoryPort.buscarPorCitaId(citaId).isPresent()) {
            throw new CitaYaCalificadaException();
        }
        boolean fueraDePlazo = fechaDeFinalizacion(cita)
                .map(finalizada -> ahora.isAfter(configuracion.limite(finalizada)))
                .orElse(false);
        if (fueraDePlazo) {
            throw new PlazoCalificacionVencidoException(configuracion.plazoDias());
        }
        return calificacionRepositoryPort.guardar(nueva);
    }

    /**
     * Cuándo se finalizó la cita: la fecha del cambio a FINALIZADA en el historial de estados. Si la cita se finalizó
     * antes de existir el historial, se usa la última actualización de la cita (nunca anterior a la finalización real).
     */
    private Optional<LocalDateTime> fechaDeFinalizacion(Cita cita) {
        Optional<LocalDateTime> delHistorial = historialPort.listarPorCita(cita.getId()).stream()
                .filter(h -> h.estadoNuevo() == EstadoCita.FINALIZADA)
                .reduce((anterior, siguiente) -> siguiente)
                .map(HistorialEstadoCita::fecha);
        return delHistorial.or(() -> Optional.ofNullable(cita.getActualizadaEn()));
    }
}
