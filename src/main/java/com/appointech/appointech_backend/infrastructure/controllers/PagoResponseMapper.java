package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ComprobanteDetalle;
import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.SolicitudPago;
import com.appointech.appointech_backend.infrastructure.dto.ComprobanteResponse;
import com.appointech.appointech_backend.infrastructure.dto.CotizacionResponse;
import com.appointech.appointech_backend.infrastructure.dto.PagoResponse;

/** Traduce los modelos de pagos del dominio a las respuestas de la API. */
final class PagoResponseMapper {

    private PagoResponseMapper() {
    }

    static PagoResponse aRespuesta(Pago pago, ConfiguracionPagos configuracion) {
        return aRespuesta(pago, null, configuracion);
    }

    static PagoResponse aRespuesta(Pago pago, String urlPago, ConfiguracionPagos configuracion) {
        return new PagoResponse(pago.getCitaId(), pago.getMontoCotizado(), pago.getMontoFinal(), pago.getMetodo(),
                pago.getEstado(), pago.getReferenciaExterna(), pago.getPagadoEn(),
                pago.diferenciaSignificativa(configuracion.umbralDiferenciaPorcentaje()),
                pago.porcentajeDiferencia(), urlPago);
    }

    static CotizacionResponse aRespuesta(Cotizacion cotizacion) {
        return new CotizacionResponse(cotizacion.especialidadId(), cotizacion.especialidad(),
                cotizacion.montoEstimado(), SolicitudPago.MONEDA_COP, cotizacion.disponible(), cotizacion.nota());
    }

    static ComprobanteResponse aRespuesta(ComprobanteDetalle detalle) {
        return new ComprobanteResponse(detalle.numero(), detalle.emitidoEn(), detalle.citaId(), detalle.cliente(),
                detalle.tecnico(), detalle.especialidad(), detalle.monto(), SolicitudPago.MONEDA_COP,
                detalle.metodo(), detalle.referencia(), detalle.pagadoEn());
    }
}
