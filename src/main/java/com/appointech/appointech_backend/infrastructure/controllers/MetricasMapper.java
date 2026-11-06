package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.infrastructure.dto.MetricasResponse;

import java.util.LinkedHashMap;
import java.util.Map;

final class MetricasMapper {

    private MetricasMapper() {
    }

    static CriteriosMetricas aCriterios(String desde, String hasta) {
        return new CriteriosMetricas(ParametrosDeConsulta.aFecha("desde", desde), ParametrosDeConsulta.aFecha("hasta", hasta));
    }

    static MetricasResponse aRespuesta(MetricasGenerales m) {
        return new MetricasResponse(
                new MetricasResponse.RangoResponse(m.rango().desde().toString(), m.rango().hasta().toString(), m.rango().dias()),
                new MetricasResponse.CitasResponse(m.citas().total(), conNombres(m.citas().porEstado()), m.citas().tasaCancelacion()),
                new MetricasResponse.SolicitudesEspecialesResponse(m.solicitudesEspeciales().total(),
                        conNombres(m.solicitudesEspeciales().porMotivo())),
                ingresos(m.ingresos()),
                m.especialidades().stream().map(e -> new MetricasResponse.EspecialidadResponse(
                        e.especialidadId(), e.nombre(), e.totalCitas(), e.finalizadas())).toList(),
                new MetricasResponse.CalificacionesResponse(m.calificaciones().promedio(), m.calificaciones().total(),
                        m.calificaciones().porTecnico().stream().map(t -> new MetricasResponse.CalificacionTecnicoResponse(
                                t.tecnicoId(), t.nombre(), t.promedio(), t.cantidad())).toList()),
                new MetricasResponse.UsuariosActivosResponse(m.usuariosActivos().total(), conNombres(m.usuariosActivos().porRol())));
    }

    private static MetricasResponse.IngresosResponse ingresos(MetricasGenerales.Ingresos i) {
        Map<String, MetricasResponse.IngresoMetodoResponse> porMetodo = new LinkedHashMap<>();
        i.porMetodo().forEach((metodo, ingreso) ->
                porMetodo.put(metodo.name(), new MetricasResponse.IngresoMetodoResponse(ingreso.monto(), ingreso.cantidad())));
        return new MetricasResponse.IngresosResponse(i.total(), i.cantidadPagos(), porMetodo,
                new MetricasResponse.PorCobrarResponse(i.porCobrar().cantidad(), i.porCobrar().monto()));
    }

    /** Claves como texto, en el orden del enum. */
    private static Map<String, Long> conNombres(Map<? extends Enum<?>, Long> valores) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        valores.forEach((clave, valor) -> resultado.put(clave.name(), valor));
        return resultado;
    }
}
