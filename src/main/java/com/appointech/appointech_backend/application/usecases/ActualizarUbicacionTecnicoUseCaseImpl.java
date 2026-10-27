package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ActualizarUbicacionTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ActualizarUbicacionTecnicoUseCaseImpl implements ActualizarUbicacionTecnicoUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public ActualizarUbicacionTecnicoUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    @Override
    public Tecnico actualizar(Long tecnicoId, Double latitud, Double longitud) {
        Coordenadas base = Coordenadas.de(latitud, longitud);
        Tecnico tecnico = tecnicoRepositoryPort.buscarPorId(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
        tecnico.setLatitudBase(base.latitud());
        tecnico.setLongitudBase(base.longitud());
        return tecnicoRepositoryPort.guardar(tecnico);
    }
}
