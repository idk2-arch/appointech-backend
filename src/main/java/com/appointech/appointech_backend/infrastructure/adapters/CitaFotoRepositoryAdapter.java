package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.CitaFotoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaFotoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CitaFotoRepositoryAdapter implements CitaFotoRepositoryPort {

    private final CitaFotoJpaRepository citaFotoJpaRepository;

    public CitaFotoRepositoryAdapter(CitaFotoJpaRepository citaFotoJpaRepository) {
        this.citaFotoJpaRepository = citaFotoJpaRepository;
    }

    @Override
    public CitaFoto guardar(CitaFoto foto) {
        CitaFotoEntity entity = new CitaFotoEntity(foto.id(), foto.citaId(), foto.clave(), foto.creadoEn());
        return aDominio(citaFotoJpaRepository.save(entity));
    }

    @Override
    public Optional<CitaFoto> buscarPorIdYCita(Long fotoId, Long citaId) {
        return citaFotoJpaRepository.findByIdAndCitaId(fotoId, citaId).map(this::aDominio);
    }

    @Override
    public List<CitaFoto> listarPorCita(Long citaId) {
        return citaFotoJpaRepository.findByCitaIdOrderByIdAsc(citaId).stream().map(this::aDominio).toList();
    }

    @Override
    public Map<Long, List<CitaFoto>> listarPorCitas(Collection<Long> citaIds) {
        if (citaIds.isEmpty()) {
            return Map.of();
        }
        return citaFotoJpaRepository.findByCitaIdInOrderByIdAsc(citaIds).stream()
                .map(this::aDominio)
                .collect(Collectors.groupingBy(CitaFoto::citaId));
    }

    @Override
    public long contarPorCita(Long citaId) {
        return citaFotoJpaRepository.countByCitaId(citaId);
    }

    @Override
    public void eliminar(Long fotoId) {
        citaFotoJpaRepository.deleteById(fotoId);
    }

    private CitaFoto aDominio(CitaFotoEntity entity) {
        return new CitaFoto(entity.getId(), entity.getCitaId(), entity.getClave(), entity.getCreadoEn());
    }
}
