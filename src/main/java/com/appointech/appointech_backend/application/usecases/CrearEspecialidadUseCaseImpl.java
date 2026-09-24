package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.exception.EspecialidadNombreDuplicadoException;
import com.appointech.appointech_backend.domain.ports.in.CrearEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CrearEspecialidadUseCaseImpl implements CrearEspecialidadUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public CrearEspecialidadUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public Especialidad crear(String nombre, String descripcion) {
        if (especialidadRepositoryPort.buscarPorNombre(nombre).isPresent()) {
            throw new EspecialidadNombreDuplicadoException(nombre);
        }

        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre);
        especialidad.setDescripcion(descripcion);
        especialidad.setActivo(true);

        return especialidadRepositoryPort.guardar(especialidad);
    }
}