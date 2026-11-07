package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.FranjaTecnicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FranjaTecnicoJpaRepository extends JpaRepository<FranjaTecnicoEntity, Long> {

    Optional<FranjaTecnicoEntity> findByIdAndTecnicoId(Long id, Long tecnicoId);

    List<FranjaTecnicoEntity> findByTecnicoIdAndFechaOrderByHoraInicioAsc(Long tecnicoId, LocalDate fecha);

    List<FranjaTecnicoEntity> findByTecnicoIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(Long tecnicoId, LocalDate desde, LocalDate hasta);

    List<FranjaTecnicoEntity> findByTecnicoIdInAndFechaOrderByHoraInicioAsc(Collection<Long> tecnicoIds, LocalDate fecha);
}
