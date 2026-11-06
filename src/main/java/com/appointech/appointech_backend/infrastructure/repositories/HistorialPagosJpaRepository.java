package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.infrastructure.entities.PagoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface HistorialPagosJpaRepository extends JpaRepository<PagoEntity, Long> {

    /**
     * Cita, cliente, técnico, especialidad y comprobante se traen con joins en la misma consulta (sin N+1).
     * Cada filtro opcional se activa con un indicador y un valor centinela no nulo (PostgreSQL no sabe tipar parámetros nulos).
     * El rango de fechas es [desde, hasta): un pago sin fecha nunca lo cumple.
     */
    @Query(value = "SELECT new com.appointech.appointech_backend.infrastructure.repositories.PagoHistorialFila("
            + "ci.id, cl.id, cu.nombre, cu.apellido, t.id, tu.nombre, tu.apellido, e.nombre, "
            + "p.montoCotizado, p.montoFinal, p.metodo, p.estado, p.pagadoEn, cp.id) "
            + "FROM PagoEntity p "
            + "JOIN CitaEntity ci ON ci.id = p.citaId "
            + "JOIN ClienteEntity cl ON cl.id = ci.clienteId JOIN cl.usuario cu "
            + "LEFT JOIN TecnicoEntity t ON t.id = ci.tecnicoId LEFT JOIN t.usuario tu "
            + "JOIN EspecialidadEntity e ON e.id = ci.especialidadId "
            + "LEFT JOIN ComprobantePagoEntity cp ON cp.pagoId = p.id "
            + "WHERE (:todosLosEstados = true OR p.estado = :estado) "
            + "AND (:todosLosMetodos = true OR p.metodo = :metodo) "
            + "AND (:todosLosClientes = true OR ci.clienteId = :clienteId) "
            + "AND (:todosLosTecnicos = true OR ci.tecnicoId = :tecnicoId) "
            + "AND (:sinFiltroDeFechas = true OR (p.pagadoEn >= :desde AND p.pagadoEn < :hasta)) "
            + "ORDER BY p.pagadoEn DESC NULLS LAST, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM PagoEntity p "
                    + "JOIN CitaEntity ci ON ci.id = p.citaId "
                    + "WHERE (:todosLosEstados = true OR p.estado = :estado) "
                    + "AND (:todosLosMetodos = true OR p.metodo = :metodo) "
                    + "AND (:todosLosClientes = true OR ci.clienteId = :clienteId) "
                    + "AND (:todosLosTecnicos = true OR ci.tecnicoId = :tecnicoId) "
                    + "AND (:sinFiltroDeFechas = true OR (p.pagadoEn >= :desde AND p.pagadoEn < :hasta))")
    Page<PagoHistorialFila> buscar(@Param("todosLosEstados") boolean todosLosEstados,
                                   @Param("estado") EstadoPago estado,
                                   @Param("todosLosMetodos") boolean todosLosMetodos,
                                   @Param("metodo") MetodoPago metodo,
                                   @Param("todosLosClientes") boolean todosLosClientes,
                                   @Param("clienteId") Long clienteId,
                                   @Param("todosLosTecnicos") boolean todosLosTecnicos,
                                   @Param("tecnicoId") Long tecnicoId,
                                   @Param("sinFiltroDeFechas") boolean sinFiltroDeFechas,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   Pageable pageable);
}
