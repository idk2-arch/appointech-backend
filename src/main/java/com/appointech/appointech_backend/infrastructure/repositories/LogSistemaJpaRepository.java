package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.LogSistemaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LogSistemaJpaRepository extends JpaRepository<LogSistemaEntity, Long> {

    List<LogSistemaEntity> findAllByOrderByCreadoEnDesc(Pageable pageable);

    List<LogSistemaEntity> findByNivelOrderByCreadoEnDesc(String nivel, Pageable pageable);

    List<LogSistemaEntity> findByTipoOrderByCreadoEnDesc(String tipo, Pageable pageable);

    List<LogSistemaEntity> findByNivelAndTipoOrderByCreadoEnDesc(String nivel, String tipo, Pageable pageable);

    @Modifying
    @Query("DELETE FROM LogSistemaEntity l WHERE l.creadoEn < :fechaLimite")
    int eliminarAnterioresA(@Param("fechaLimite") LocalDateTime fechaLimite);
}
