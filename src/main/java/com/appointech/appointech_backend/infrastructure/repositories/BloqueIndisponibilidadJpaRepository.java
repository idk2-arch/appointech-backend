package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BloqueIndisponibilidadJpaRepository extends JpaRepository<BloqueIndisponibilidadEntity, Long> {

    List<BloqueIndisponibilidadEntity> findByTecnicoIdInAndInicioLessThanAndFinGreaterThan(
            Collection<Long> tecnicoIds, LocalDateTime fin, LocalDateTime inicio);

    List<BloqueIndisponibilidadEntity> findByTecnicoIdAndInicioLessThanAndFinGreaterThan(
            Long tecnicoId, LocalDateTime fin, LocalDateTime inicio);
}
