package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.HistorialEstadoCitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoCitaJpaRepository extends JpaRepository<HistorialEstadoCitaEntity, Long> {

    List<HistorialEstadoCitaEntity> findByCitaIdOrderByFechaAscIdAsc(Long citaId);
}
