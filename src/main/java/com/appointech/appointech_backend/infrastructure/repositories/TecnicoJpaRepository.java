package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.TecnicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TecnicoJpaRepository extends JpaRepository<TecnicoEntity, Long> {

    Optional<TecnicoEntity> findByUsuarioId(Long usuarioId);
}