package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.NotificacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {

    Page<NotificacionEntity> findByUsuarioIdOrderByCreadaEnDescIdDesc(Long usuarioId, Pageable pageable);

    Page<NotificacionEntity> findByUsuarioIdAndLeidaFalseOrderByCreadaEnDescIdDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Optional<NotificacionEntity> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying
    @Query("UPDATE NotificacionEntity n SET n.leida = true WHERE n.usuarioId = :usuarioId AND n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}
