package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.infrastructure.dto.PagoHistorialResponse;

final class HistorialPagosMapper {

    private HistorialPagosMapper() {
    }

    static FiltroHistorialPagos aFiltro(String estado, String metodo, Long clienteId, Long tecnicoId,
                                        String desde, String hasta, Integer page, Integer size) {
        return new FiltroHistorialPagos(
                ParametrosDeConsulta.aEnum(EstadoPago.class, "estado", estado),
                ParametrosDeConsulta.aEnum(MetodoPago.class, "metodo", metodo),
                clienteId, tecnicoId,
                ParametrosDeConsulta.aFecha("desde", desde),
                ParametrosDeConsulta.aFecha("hasta", hasta),
                page, size);
    }

    static PagoHistorialResponse aRespuesta(PagoDelHistorial p) {
        return new PagoHistorialResponse(p.citaId(), p.clienteId(), p.clienteNombre(), p.tecnicoId(), p.tecnicoNombre(),
                p.especialidad(), p.montoCotizado(), p.montoFinal(),
                p.metodo() == null ? null : p.metodo().name(), p.estado().name(),
                p.pagadoEn(), p.numeroComprobante());
    }
}
