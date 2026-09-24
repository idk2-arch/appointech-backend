package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EspecialidadAdminInfo;
import com.appointech.appointech_backend.domain.ports.in.ListarEspecialidadesAdminUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarEspecialidadesAdminUseCaseImpl implements ListarEspecialidadesAdminUseCase {

    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ListarEspecialidadesAdminUseCaseImpl(EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public List<EspecialidadAdminInfo> listar() {
        List<Especialidad> especialidades = especialidadRepositoryPort.listarTodas();

        return especialidades.stream()
                .map(e -> new EspecialidadAdminInfo(e, especialidadRepositoryPort.contarTecnicosAsignados(e.getId())))
                .toList();
    }
}