package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.BloqueIndisponibilidadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Consultas de gestión de bloques (listar y eliminar), separadas del repositorio que usa el cálculo de franjas. */
public interface GestionBloquesJpaRepository extends JpaRepository<BloqueIndisponibilidadEntity, Long> {

    List<BloqueIndisponibilidadEntity> findByTecnicoIdAndFinGreaterThanOrderByInicioAsc(
            Long tecnicoId, LocalDateTime desde);

    List<BloqueIndisponibilidadEntity> findByTecnicoIdAndFinGreaterThanAndInicioLessThanOrderByInicioAsc(
            Long tecnicoId, LocalDateTime desde, LocalDateTime hasta);

    @Modifying
    @Query("DELETE FROM BloqueIndisponibilidadEntity b WHERE b.id = :id AND b.tecnicoId = :tecnicoId")
    int eliminarDelTecnico(@Param("id") Long id, @Param("tecnicoId") Long tecnicoId);
}
