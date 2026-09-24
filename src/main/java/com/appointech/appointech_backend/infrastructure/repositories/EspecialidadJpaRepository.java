package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EspecialidadJpaRepository extends JpaRepository<EspecialidadEntity, Long> {
    List<EspecialidadEntity> findByActivoTrue();
    Optional<EspecialidadEntity> findByNombre(String nombre);

    @Query(value = "SELECT COUNT(*) FROM tecnico_especialidad WHERE especialidad_id = :especialidadId", nativeQuery = true)
    long contarTecnicosPorEspecialidad(@Param("especialidadId") Long especialidadId);
}