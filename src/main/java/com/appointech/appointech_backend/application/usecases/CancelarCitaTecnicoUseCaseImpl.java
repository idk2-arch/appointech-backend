package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el técnico asignado cancela la cita con un motivo obligatorio.
 * <p>Historias de usuario: SCRUM-21 (cancelar), SCRUM-20 (notificar al cliente).
 */
@Service
public class CancelarCitaTecnicoUseCaseImpl implements CancelarCitaTecnicoUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final CitaRepositoryPort citaRepositoryPort;
    private final GestorCambiosCita gestor;

    public CancelarCitaTecnicoUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                          CitaRepositoryPort citaRepositoryPort,
                                          GestorCambiosCita gestor) {
        this.resolverTecnico = resolverTecnico;
        this.citaRepositoryPort = citaRepositoryPort;
        this.gestor = gestor;
    }

    @Override
    @Transactional
    public Cita cancelar(String correoTecnico, Long citaId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new DatosCitaInvalidosException("El motivo de la cancelación es obligatorio");
        }
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> tecnico.getId().equals(c.getTecnicoId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));

        EstadoCita anterior = cita.getEstado();
        cita.cancelarPorTecnico();
        String motivoLimpio = motivo.strip();
        Cita guardada = gestor.guardarConHistorial(cita, anterior, correoTecnico, motivoLimpio);

        gestor.notificarAlCliente(guardada, "El técnico canceló tu cita #" + guardada.getId()
                + ". Motivo: " + motivoLimpio);
        return guardada;
    }
}
