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

    Optional<TecnicoEntity> findByUsuarioId(Long usuarioId);

    @Query("SELECT DISTINCT t FROM TecnicoEntity t JOIN t.especialidades e "
            + "WHERE e.id = :especialidadId AND t.usuario.activo = true")
    List<TecnicoEntity> buscarActivosPorEspecialidad(@Param("especialidadId") Long especialidadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TecnicoEntity t WHERE t.id = :id")
    Optional<TecnicoEntity> buscarPorIdBloqueando(@Param("id") Long id);
}