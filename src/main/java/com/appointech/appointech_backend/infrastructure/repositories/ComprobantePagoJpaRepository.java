package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.ComprobantePagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobantePagoJpaRepository extends JpaRepository<ComprobantePagoEntity, Long> {

    Optional<ComprobantePagoEntity> findByPagoId(Long pagoId);
}
