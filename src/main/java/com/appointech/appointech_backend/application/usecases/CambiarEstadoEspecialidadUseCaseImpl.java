package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.ports.in.CambiarEstadoEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CambiarEstadoEspecialidadUseCaseImpl implements CambiarEstadoEspecialidadUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public CambiarEstadoEspecialidadUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public Especialidad cambiarEstado(Long id, boolean activo) {
        Especialidad especialidad = especialidadRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(id)));

        especialidad.setActivo(activo);

        return especialidadRepositoryPort.guardar(especialidad);
    }
}