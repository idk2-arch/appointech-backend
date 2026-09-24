package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.ListarTecnicosUseCase;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarTecnicosUseCaseImpl implements ListarTecnicosUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;

    public ListarTecnicosUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
    }

    @Override
    public List<Tecnico> listar() {
        return tecnicoRepositoryPort.listarTodos();
    }
}