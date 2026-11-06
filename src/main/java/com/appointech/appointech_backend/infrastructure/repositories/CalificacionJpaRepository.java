package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.CalificacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CalificacionJpaRepository extends JpaRepository<CalificacionEntity, Long> {

    Optional<CalificacionEntity> findByCitaId(Long citaId);

    /** Calificaciones de las citas del técnico, con el cliente en la misma consulta (sin N+1), las más recientes primero. */
    @Query(value = "SELECT new com.appointech.appointech_backend.infrastructure.repositories.CalificacionRecibidaFila("
            + "c.citaId, c.puntuacion, c.comentario, c.creadaEn, cu.nombre) "
            + "FROM CalificacionEntity c "
            + "JOIN CitaEntity ci ON ci.id = c.citaId "
            + "JOIN ClienteEntity cl ON cl.id = ci.clienteId JOIN cl.usuario cu "
            + "WHERE ci.tecnicoId = :tecnicoId "
            + "ORDER BY c.creadaEn DESC, c.id DESC",
            countQuery = "SELECT COUNT(c) FROM CalificacionEntity c JOIN CitaEntity ci ON ci.id = c.citaId "
                    + "WHERE ci.tecnicoId = :tecnicoId")
    Page<CalificacionRecibidaFila> buscarRecibidasPorTecnico(@Param("tecnicoId") Long tecnicoId, Pageable pageable);

    /** Null si el técnico no tiene calificaciones. */
    @Query("SELECT AVG(c.puntuacion) FROM CalificacionEntity c JOIN CitaEntity ci ON ci.id = c.citaId "
            + "WHERE ci.tecnicoId = :tecnicoId")
    Double promedioDelTecnico(@Param("tecnicoId") Long tecnicoId);
}
