package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ComprobanteDetalle;

public interface ConsultarComprobanteUseCase {
    /** Consulta el comprobante ya generado; nunca crea uno ni existe si el pago no está PAGADO. */
    ComprobanteDetalle consultar(String correo, Long citaId);
}
