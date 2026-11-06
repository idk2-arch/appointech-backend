package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaJpaRepository extends JpaRepository<CitaEntity, Long> {

    List<CitaEntity> findByClienteIdOrderByCreadaEnDescIdDesc(Long clienteId);

    /**
     * Solicitudes manuales del estado dado. Los filtros opcionales se activan con un indicador y un valor
     * centinela no nulo (evita parámetros nulos, que PostgreSQL no sabe tipar).
     */
    @Query("SELECT c FROM CitaEntity c WHERE c.agendamientoManual = true AND c.estado = :estado "
            + "AND (:todosLosMotivos = true OR c.motivoSolicitudEspecial = :motivo) "
            + "AND (:todasLasEspecialidades = true OR c.especialidadId = :especialidadId) "
            + "ORDER BY c.creadaEn ASC, c.id ASC")
    Page<CitaEntity> buscarSolicitudesManuales(@Param("estado") EstadoCitaEntity estado,
                                               @Param("todosLosMotivos") boolean todosLosMotivos,
                                               @Param("motivo") String motivo,
                                               @Param("todasLasEspecialidades") boolean todasLasEspecialidades,
                                               @Param("especialidadId") Long especialidadId,
                                               Pageable pageable);

    @Query("SELECT c FROM CitaEntity c WHERE c.tecnicoId = :tecnicoId AND c.estado <> :estadoExcluido "
            + "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio")
    List<CitaEntity> buscarDelTecnicoEntre(@Param("tecnicoId") Long tecnicoId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin,
                                           @Param("estadoExcluido") EstadoCitaEntity estadoExcluido);
}
