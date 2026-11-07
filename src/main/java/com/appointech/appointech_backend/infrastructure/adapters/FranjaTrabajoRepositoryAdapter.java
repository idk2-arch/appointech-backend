package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.FranjaTecnicoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.FranjaTecnicoJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class FranjaTrabajoRepositoryAdapter implements FranjaTrabajoRepositoryPort {

    private final FranjaTecnicoJpaRepository franjaJpaRepository;

    public FranjaTrabajoRepositoryAdapter(FranjaTecnicoJpaRepository franjaJpaRepository) {
        this.franjaJpaRepository = franjaJpaRepository;
    }

    @Override
    public FranjaTrabajo guardar(FranjaTrabajo franja) {
        FranjaTecnicoEntity entity = new FranjaTecnicoEntity(franja.id(), franja.tecnicoId(), franja.fecha(),
                franja.horaInicio(), franja.horaFin(), franja.creadaPorId(), franja.creadaEn());
        return aDominio(franjaJpaRepository.save(entity));
    }

    @Override
    public Optional<FranjaTrabajo> buscarDelTecnico(Long franjaId, Long tecnicoId) {
        return franjaJpaRepository.findByIdAndTecnicoId(franjaId, tecnicoId).map(FranjaTrabajoRepositoryAdapter::aDominio);
    }

    @Override
    public List<FranjaTrabajo> buscarDelTecnicoEnFecha(Long tecnicoId, LocalDate fecha) {
        return franjaJpaRepository.findByTecnicoIdAndFechaOrderByHoraInicioAsc(tecnicoId, fecha).stream()
                .map(FranjaTrabajoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<FranjaTrabajo> buscarDelTecnicoEntre(Long tecnicoId, LocalDate desde, LocalDate hasta) {
        return franjaJpaRepository.findByTecnicoIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(tecnicoId, desde, hasta).stream()
                .map(FranjaTrabajoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<FranjaTrabajo> buscarDeTecnicosEnFecha(Collection<Long> tecnicoIds, LocalDate fecha) {
        if (tecnicoIds.isEmpty()) {
            return List.of();
        }
        return franjaJpaRepository.findByTecnicoIdInAndFechaOrderByHoraInicioAsc(tecnicoIds, fecha).stream()
                .map(FranjaTrabajoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public void eliminar(Long franjaId) {
        franjaJpaRepository.deleteById(franjaId);
    }

    private static FranjaTrabajo aDominio(FranjaTecnicoEntity entity) {
        return new FranjaTrabajo(entity.getId(), entity.getTecnicoId(), entity.getFecha(), entity.getHoraInicio(),
                entity.getHoraFin(), entity.getCreadaPorId(), entity.getCreadaEn());
    }
}
