package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.ports.in.ActualizarEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActualizarEspecialidadUseCaseImpl implements ActualizarEspecialidadUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ActualizarEspecialidadUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public Especialidad actualizar(Long id, String nombre, String descripcion) {
        Especialidad especialidad = especialidadRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(id)));

        especialidadRepositoryPort.buscarPorNombre(nombre).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new EspecialidadNombreDuplicadoException(nombre);
            }
        });

        especialidad.setNombre(nombre);
        especialidad.setDescripcion(descripcion);

        return especialidadRepositoryPort.guardar(especialidad);
    }
}