package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;

public interface ConsultarMetricasUseCase {

    /**
     * Métricas generales del rango pedido (por defecto, los últimos 30 días hasta hoy).
     *
     * @throws com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException si desde es posterior a hasta o el rango supera 366 días
     */
    MetricasGenerales consultar(CriteriosMetricas criterios);
}
