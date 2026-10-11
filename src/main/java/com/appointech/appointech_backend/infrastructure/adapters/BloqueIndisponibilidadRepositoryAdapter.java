package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import com.appointech.appointech_backend.infrastructure.repositories.BloqueIndisponibilidadJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BloqueIndisponibilidadRepositoryAdapter implements BloqueIndisponibilidadRepositoryPort {

    private final BloqueIndisponibilidadJpaRepository bloqueIndisponibilidadJpaRepository;

    public BloqueIndisponibilidadRepositoryAdapter(BloqueIndisponibilidadJpaRepository bloqueIndisponibilidadJpaRepository) {
        this.bloqueIndisponibilidadJpaRepository = bloqueIndisponibilidadJpaRepository;
    }

    @Override
    public List<BloqueIndisponibilidad> buscarDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
        return bloqueIndisponibilidadJpaRepository
                .findByTecnicoIdAndInicioLessThanAndFinGreaterThan(tecnicoId, fin, inicio)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private BloqueIndisponibilidad toDomain(BloqueIndisponibilidadEntity entity) {
        return new BloqueIndisponibilidad(
                entity.getId(), entity.getTecnicoId(), entity.getInicio(), entity.getFin(), entity.getMotivo());
    }
}
