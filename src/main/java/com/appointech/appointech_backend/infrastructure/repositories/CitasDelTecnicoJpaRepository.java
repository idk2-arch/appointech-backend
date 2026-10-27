package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CitasDelTecnicoJpaRepository extends JpaRepository<CitaEntity, Long> {

    @Query("SELECT c FROM CitaEntity c WHERE c.tecnicoId = :tecnicoId AND c.estado IN :estados "
            + "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio ORDER BY c.fechaHoraInicio ASC, c.id ASC")
    List<CitaEntity> buscarPorEstadosEntre(@Param("tecnicoId") Long tecnicoId,
                                           @Param("estados") Collection<EstadoCitaEntity> estados,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin);
}
