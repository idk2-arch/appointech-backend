package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoJpaRepository extends JpaRepository<PagoEntity, Long> {

    Optional<PagoEntity> findByCitaId(Long citaId);
}
