package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.Usuario;

import java.util.List;

public interface ListarUsuariosUseCase {
    List<Usuario> listar();

    /** Una página de usuarios (ordenados por id) con el total de usuarios existentes. */
    ListadoConTotal<Usuario> listarPagina(PaginacionOpcional paginacion);
}