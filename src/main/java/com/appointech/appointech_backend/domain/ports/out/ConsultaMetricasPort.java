package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;

import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas de las métricas (GROUP BY, SUM, AVG, COUNT en la base). Los mapas solo traen las claves que
 * tienen datos: completar con ceros es responsabilidad del caso de uso.
 */
public interface ConsultaMetricasPort {

    /** Citas creadas en el rango, por estado. */
    Map<EstadoCita, Long> citasPorEstado(RangoMetricas rango);

    /** Solicitudes especiales PENDIENTES ahora, por motivo. */
    Map<MotivoSolicitudEspecial, Long> solicitudesEspecialesPendientes();

    /** Pagos PAGADO con fecha de pago en el rango, por método. */
    Map<MetodoPago, MetricasGenerales.IngresoDeMetodo> ingresosPorMetodo(RangoMetricas rango);

    /** Pagos PENDIENTES que ya tienen costo final registrado (ahora). */
    MetricasGenerales.PagosPorCobrar pagosPorCobrar();

    /** Todas las especialidades con sus citas creadas en el rango (cero si no tuvieron), ordenadas por nombre. */
    List<MetricasGenerales.CitasDeEspecialidad> citasPorEspecialidad(RangoMetricas rango);

    /** Promedio (sin redondear, null si no hay) y cantidad de calificaciones dadas en el rango. */
    MetricasGenerales.ResumenCalificaciones calificacionesDelRango(RangoMetricas rango);

    /** Promedio (sin redondear) y cantidad por técnico con calificaciones en el rango, de mayor a menor promedio. */
    List<MetricasGenerales.CalificacionesDeTecnico> calificacionesPorTecnico(RangoMetricas rango);

    /** Usuarios activos ahora, por rol. */
    Map<RolUsuario, Long> usuariosActivosPorRol();
}
