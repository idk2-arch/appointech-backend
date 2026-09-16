package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.ports.in.ListarEspecialidadesUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarEspecialidadesUseCaseImpl implements ListarEspecialidadesUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ListarEspecialidadesUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public List<Especialidad> listar() {
        return especialidadRepositoryPort.listarTodas();
    }
}