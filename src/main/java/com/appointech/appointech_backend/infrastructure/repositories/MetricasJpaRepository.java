package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas agregadas de las métricas. Los rangos son [desde, hasta): el adaptador pasa el inicio del día siguiente al
 * último día para que ese día entre completo.
 */
public interface MetricasJpaRepository extends JpaRepository<CitaEntity, Long> {

    String FILAS = "com.appointech.appointech_backend.infrastructure.repositories.MetricasFilas$";

    @Query("SELECT new " + FILAS + "ConteoEstado(c.estado, COUNT(c)) FROM CitaEntity c "
            + "WHERE c.creadaEn >= :desde AND c.creadaEn < :hasta GROUP BY c.estado")
    List<MetricasFilas.ConteoEstado> citasPorEstado(@Param("desde") LocalDateTime desde,
                                                    @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new " + FILAS + "ConteoMotivo(c.motivoSolicitudEspecial, COUNT(c)) FROM CitaEntity c "
            + "WHERE c.agendamientoManual = true AND c.estado = :pendiente AND c.motivoSolicitudEspecial IS NOT NULL "
            + "GROUP BY c.motivoSolicitudEspecial")
    List<MetricasFilas.ConteoMotivo> solicitudesEspecialesPorMotivo(@Param("pendiente") EstadoCitaEntity pendiente);

    @Query("SELECT new " + FILAS + "IngresoMetodo(p.metodo, SUM(p.montoFinal), COUNT(p)) FROM PagoEntity p "
            + "WHERE p.estado = :pagado AND p.pagadoEn >= :desde AND p.pagadoEn < :hasta GROUP BY p.metodo")
    List<MetricasFilas.IngresoMetodo> ingresosPorMetodo(@Param("pagado") EstadoPago pagado,
                                                        @Param("desde") LocalDateTime desde,
                                                        @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new " + FILAS + "PorCobrar(COUNT(p), SUM(p.montoFinal)) FROM PagoEntity p "
            + "WHERE p.estado = :pendiente AND p.montoFinal IS NOT NULL")
    MetricasFilas.PorCobrar pagosPorCobrar(@Param("pendiente") EstadoPago pendiente);

    /** Parte de las especialidades (LEFT JOIN) para que las que no tuvieron citas aparezcan con cero. */
    @Query("SELECT new " + FILAS + "EspecialidadConteo(e.id, e.nombre, COUNT(c.id), "
            + "SUM(CASE WHEN c.estado = :finalizada THEN 1L ELSE 0L END)) "
            + "FROM EspecialidadEntity e "
            + "LEFT JOIN CitaEntity c ON c.especialidadId = e.id AND c.creadaEn >= :desde AND c.creadaEn < :hasta "
            + "GROUP BY e.id, e.nombre ORDER BY e.nombre ASC, e.id ASC")
    List<MetricasFilas.EspecialidadConteo> citasPorEspecialidad(@Param("finalizada") EstadoCitaEntity finalizada,
                                                                @Param("desde") LocalDateTime desde,
                                                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new " + FILAS + "ResumenCalificaciones(AVG(c.puntuacion), COUNT(c)) FROM CalificacionEntity c "
            + "WHERE c.creadaEn >= :desde AND c.creadaEn < :hasta")
    MetricasFilas.ResumenCalificaciones calificacionesDelRango(@Param("desde") LocalDateTime desde,
                                                              @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new " + FILAS + "CalificacionTecnico(t.id, tu.nombre, tu.apellido, AVG(c.puntuacion), COUNT(c)) "
            + "FROM CalificacionEntity c "
            + "JOIN CitaEntity ci ON ci.id = c.citaId "
            + "JOIN TecnicoEntity t ON t.id = ci.tecnicoId JOIN t.usuario tu "
            + "WHERE c.creadaEn >= :desde AND c.creadaEn < :hasta "
            + "GROUP BY t.id, tu.nombre, tu.apellido "
            + "ORDER BY AVG(c.puntuacion) DESC, COUNT(c) DESC, t.id ASC")
    List<MetricasFilas.CalificacionTecnico> calificacionesPorTecnico(@Param("desde") LocalDateTime desde,
                                                                     @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new " + FILAS + "ConteoRol(u.rol, COUNT(u)) FROM UsuarioEntity u WHERE u.activo = true GROUP BY u.rol")
    List<MetricasFilas.ConteoRol> usuariosActivosPorRol();
}
