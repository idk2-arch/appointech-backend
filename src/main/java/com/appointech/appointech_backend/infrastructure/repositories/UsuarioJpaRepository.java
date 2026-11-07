package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByCorreo(String correo);

    Optional<UsuarioEntity> findByNumeroDocumento(String numeroDocumento);

    @Query("SELECT u.id FROM UsuarioEntity u WHERE u.rol = :rol AND u.activo = true ORDER BY u.id")
    List<Long> idsActivosPorRol(@Param("rol") RolUsuarioEntity rol);
}
