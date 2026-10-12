package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.ConfiguracionFranjasEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionFranjasJpaRepository extends JpaRepository<ConfiguracionFranjasEntity, Long> {

    Optional<ConfiguracionFranjasEntity> findFirstByOrderByIdDesc();
}
