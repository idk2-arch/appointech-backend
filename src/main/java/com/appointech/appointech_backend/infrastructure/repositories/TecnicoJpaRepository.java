package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.TecnicoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TecnicoJpaRepository extends JpaRepository<TecnicoEntity, Long> {

    @Override
    @Query("SELECT DISTINCT t FROM TecnicoEntity t JOIN FETCH t.usuario LEFT JOIN FETCH t.especialidades WHERE t.id = :id")
    Optional<TecnicoEntity> findById(@Param("id") Long id);

    @Override
    @Query("SELECT DISTINCT t FROM TecnicoEntity t JOIN FETCH t.usuario LEFT JOIN FETCH t.especialidades")
    List<TecnicoEntity> findAll();

    @Query("SELECT DISTINCT t FROM TecnicoEntity t JOIN FETCH t.usuario LEFT JOIN FETCH t.especialidades WHERE t.usuario.id = :usuarioId")
    Optional<TecnicoEntity> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Trae en la misma consulta el usuario y TODAS las especialidades de cada técnico (evita una consulta por técnico al
    // armar el dominio); el filtro por especialidad va en la subconsulta para no recortar su colección.
    @Query("SELECT DISTINCT t FROM TecnicoEntity t JOIN FETCH t.usuario u JOIN FETCH t.especialidades "
            + "WHERE u.activo = true AND t.id IN "
            + "(SELECT t2.id FROM TecnicoEntity t2 JOIN t2.especialidades e2 WHERE e2.id = :especialidadId)")
    List<TecnicoEntity> buscarActivosPorEspecialidad(@Param("especialidadId") Long especialidadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TecnicoEntity t WHERE t.id = :id")
    Optional<TecnicoEntity> buscarPorIdBloqueando(@Param("id") Long id);
}
