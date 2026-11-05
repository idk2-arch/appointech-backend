package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Pago;

public interface SincronizarPagoUseCase {
    /**
     * Consulta a la pasarela y actualiza el pago en línea; genera el comprobante si queda PAGADO. Si idTransaccion es
     * null o vacío, consulta por la referencia guardada en el pago.
     */
    Pago sincronizar(String correo, Long citaId, String idTransaccion);
}
