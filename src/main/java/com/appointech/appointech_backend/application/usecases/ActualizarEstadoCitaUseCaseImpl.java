package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ActualizarEstadoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el técnico avanza el estado de su cita (en camino, en proceso, finalizada).
 * <p>Historias de usuario: SCRUM-19 (actualizar estado), SCRUM-20 (notificar al cliente).
 */
@Service
public class ActualizarEstadoCitaUseCaseImpl implements ActualizarEstadoCitaUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final CitaRepositoryPort citaRepositoryPort;
    private final GestorCambiosCita gestor;

    public ActualizarEstadoCitaUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                           CitaRepositoryPort citaRepositoryPort,
                                           GestorCambiosCita gestor) {
        this.resolverTecnico = resolverTecnico;
        this.citaRepositoryPort = citaRepositoryPort;
        this.gestor = gestor;
    }

    @Override
    @Transactional
    public Cita actualizar(String correoTecnico, Long citaId, EstadoCita nuevoEstado) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> tecnico.getId().equals(c.getTecnicoId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));

        EstadoCita anterior = cita.getEstado();
        cita.avanzarA(nuevoEstado);
        Cita guardada = gestor.guardarConHistorial(cita, anterior, correoTecnico, null);

        gestor.notificarAlCliente(guardada, "Tu cita #" + guardada.getId() + " cambió de estado: "
                + anterior + " → " + guardada.getEstado());
        return guardada;
    }
}
