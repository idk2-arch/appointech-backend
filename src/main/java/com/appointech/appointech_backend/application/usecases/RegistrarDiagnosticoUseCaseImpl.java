package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.RegistrarDiagnosticoUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el técnico registra (o corrige) el diagnóstico mientras la cita está EN_PROCESO.
 * <p>Historias de usuario: SCRUM-22 (diagnóstico).
 */
@Service
public class RegistrarDiagnosticoUseCaseImpl implements RegistrarDiagnosticoUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final CitaRepositoryPort citaRepositoryPort;
    private final GestorCambiosCita gestor;

    public RegistrarDiagnosticoUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                           CitaRepositoryPort citaRepositoryPort,
                                           GestorCambiosCita gestor) {
        this.resolverTecnico = resolverTecnico;
        this.citaRepositoryPort = citaRepositoryPort;
        this.gestor = gestor;
    }

    @Override
    @Transactional
    public Cita registrar(String correoTecnico, Long citaId, String diagnostico, String recomendaciones) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> tecnico.getId().equals(c.getTecnicoId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));

        cita.registrarDiagnostico(diagnostico, recomendaciones, gestor.ahora());
        return gestor.guardar(cita);
    }
}
