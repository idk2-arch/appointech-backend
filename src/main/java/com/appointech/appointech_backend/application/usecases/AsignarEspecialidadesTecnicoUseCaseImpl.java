package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsignarEspecialidadesTecnicoUseCaseImpl implements AsignarEspecialidadesTecnicoUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public AsignarEspecialidadesTecnicoUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort,
                                                   EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public Tecnico asignar(Long tecnicoId, List<Long> especialidadIds) {
        Tecnico tecnico = tecnicoRepositoryPort.buscarPorId(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));

        List<Especialidad> especialidades = especialidadRepositoryPort.buscarPorIds(especialidadIds);

        if (especialidades.size() != especialidadIds.size()) {
            List<Long> encontrados = especialidades.stream().map(Especialidad::getId).toList();
            List<Long> noEncontrados = especialidadIds.stream()
                    .filter(id -> !encontrados.contains(id))
                    .toList();
            throw new EspecialidadNoEncontradaException(noEncontrados);
        }

        tecnico.setEspecialidades(especialidades);

        return tecnicoRepositoryPort.guardar(tecnico);
    }
}