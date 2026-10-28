package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface CitasAdminJpaRepository extends JpaRepository<CitaEntity, Long> {

    /** Cliente, técnico y especialidad se traen con joins en la misma consulta (sin N+1). */
    @Query(value = "SELECT new com.appointech.appointech_backend.infrastructure.repositories.CitaAdminFila("
            + "c.id, c.fechaHoraInicio, c.fechaHoraFin, c.estado, cu.nombre, cu.apellido, tu.nombre, tu.apellido, "
            + "e.nombre, c.direccionServicio, tu.fotoClave) "
            + "FROM CitaEntity c "
            + "JOIN ClienteEntity cl ON cl.id = c.clienteId JOIN cl.usuario cu "
            + "LEFT JOIN TecnicoEntity t ON t.id = c.tecnicoId LEFT JOIN t.usuario tu "
            + "JOIN EspecialidadEntity e ON e.id = c.especialidadId "
            + "WHERE (:todosLosTecnicos = true OR c.tecnicoId = :tecnicoId) AND c.estado IN :estados "
            + "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta "
            + "ORDER BY c.fechaHoraInicio DESC, c.id DESC",
            countQuery = "SELECT COUNT(c) FROM CitaEntity c "
                    + "WHERE (:todosLosTecnicos = true OR c.tecnicoId = :tecnicoId) AND c.estado IN :estados "
                    + "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta")
    Page<CitaAdminFila> buscar(@Param("todosLosTecnicos") boolean todosLosTecnicos,
                               @Param("tecnicoId") Long tecnicoId,
                               @Param("estados") Collection<EstadoCitaEntity> estados,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta,
                               Pageable pageable);
}
