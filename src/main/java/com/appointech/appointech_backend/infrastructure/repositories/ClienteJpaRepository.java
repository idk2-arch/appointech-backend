package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    @Override
    @Query("SELECT c FROM ClienteEntity c JOIN FETCH c.usuario WHERE c.id IN :ids")
    List<ClienteEntity> findAllById(@Param("ids") Iterable<Long> ids);

    @Override
    @Query("SELECT c FROM ClienteEntity c JOIN FETCH c.usuario WHERE c.id = :id")
    Optional<ClienteEntity> findById(@Param("id") Long id);

    @Override
    @Query("SELECT c FROM ClienteEntity c JOIN FETCH c.usuario")
    List<ClienteEntity> findAll();

    @Query("SELECT c FROM ClienteEntity c JOIN FETCH c.usuario WHERE c.usuario.id = :usuarioId")
    Optional<ClienteEntity> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
