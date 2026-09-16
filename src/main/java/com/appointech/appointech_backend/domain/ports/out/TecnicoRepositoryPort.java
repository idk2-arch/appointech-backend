package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Tecnico;

import java.util.Optional;

public interface TecnicoRepositoryPort {
    Tecnico guardar(Tecnico tecnico);
    Optional<Tecnico> buscarPorUsuarioId(Long usuarioId);
    Optional<Tecnico> buscarPorId(Long id);
}