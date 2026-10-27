package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.ports.out.GestionBloquesPort;
import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import com.appointech.appointech_backend.infrastructure.repositories.GestionBloquesJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GestionBloquesAdapter implements GestionBloquesPort {

    private final GestionBloquesJpaRepository gestionBloquesJpaRepository;

    public GestionBloquesAdapter(GestionBloquesJpaRepository gestionBloquesJpaRepository) {
        this.gestionBloquesJpaRepository = gestionBloquesJpaRepository;
    }

    @Override
    public BloqueIndisponibilidad guardar(BloqueIndisponibilidad bloque) {
        BloqueIndisponibilidadEntity entity = new BloqueIndisponibilidadEntity();
        entity.setId(bloque.getId());
        entity.setTecnicoId(bloque.getTecnicoId());
        entity.setInicio(bloque.getInicio());
        entity.setFin(bloque.getFin());
        entity.setMotivo(bloque.getMotivo());
        return aDominio(gestionBloquesJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueIndisponibilidad> listarDelTecnico(Long tecnicoId, LocalDateTime desde, LocalDateTime hasta) {
        List<BloqueIndisponibilidadEntity> entidades = hasta == null
                ? gestionBloquesJpaRepository.findByTecnicoIdAndFinGreaterThanOrderByInicioAsc(tecnicoId, desde)
                : gestionBloquesJpaRepository
                .findByTecnicoIdAndFinGreaterThanAndInicioLessThanOrderByInicioAsc(tecnicoId, desde, hasta);
        return entidades.stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional
    public boolean eliminarDelTecnico(Long bloqueId, Long tecnicoId) {
        return gestionBloquesJpaRepository.eliminarDelTecnico(bloqueId, tecnicoId) > 0;
    }

    private BloqueIndisponibilidad aDominio(BloqueIndisponibilidadEntity entity) {
        return new BloqueIndisponibilidad(
                entity.getId(), entity.getTecnicoId(), entity.getInicio(), entity.getFin(), entity.getMotivo());
    }
}
