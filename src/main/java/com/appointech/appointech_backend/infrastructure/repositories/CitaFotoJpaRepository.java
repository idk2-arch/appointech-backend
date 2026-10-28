package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CitaFotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaFotoJpaRepository extends JpaRepository<CitaFotoEntity, Long> {

    Optional<CitaFotoEntity> findByIdAndCitaId(Long id, Long citaId);

    List<CitaFotoEntity> findByCitaIdOrderByIdAsc(Long citaId);

    List<CitaFotoEntity> findByCitaIdInOrderByIdAsc(Collection<Long> citaIds);

    long countByCitaId(Long citaId);
}
