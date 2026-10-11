package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: obtener los técnicos activos que atienden una especialidad.
 * <p>Historias de usuario:
 * <ul>
 * <li><a href="https://TU-SITIO.atlassian.net/browse/SCRUM-11">SCRUM-11</a> - RF27 - Filtrar técnicos por especialidad al calcular disponibilidad</li>
 * </ul>
 */
@Service
public class FiltrarTecnicosPorEspecialidadUseCaseImpl implements FiltrarTecnicosPorEspecialidadUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public FiltrarTecnicosPorEspecialidadUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    @Override
    public List<Tecnico> filtrar(Long especialidadId) {
        return tecnicoRepositoryPort.buscarActivosPorEspecialidad(especialidadId);
    }
}
