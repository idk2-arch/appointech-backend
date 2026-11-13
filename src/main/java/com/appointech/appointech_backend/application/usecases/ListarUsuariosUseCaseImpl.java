package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ListarUsuariosUseCase;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarUsuariosUseCaseImpl implements ListarUsuariosUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ListarUsuariosUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepositoryPort.listarTodos();
    }

    @Override
    public ListadoConTotal<Usuario> listarPagina(PaginacionOpcional paginacion) {
        return usuarioRepositoryPort.listarPagina(paginacion.pagina(), paginacion.tamanio());
    }
}