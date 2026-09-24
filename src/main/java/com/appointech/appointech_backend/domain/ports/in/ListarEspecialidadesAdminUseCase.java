package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.EspecialidadAdminInfo;

import java.util.List;

public interface ListarEspecialidadesAdminUseCase {
    List<EspecialidadAdminInfo> listar();
}