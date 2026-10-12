package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaJpaRepository extends JpaRepository<CitaEntity, Long> {

    @Query("SELECT c FROM CitaEntity c WHERE c.tecnicoId = :tecnicoId AND c.estado <> :estadoExcluido "
            + "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio")
    List<CitaEntity> buscarDelTecnicoEntre(@Param("tecnicoId") Long tecnicoId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin,
                                           @Param("estadoExcluido") EstadoCitaEntity estadoExcluido);
}
