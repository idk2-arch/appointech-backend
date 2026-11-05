package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.models.SolicitudPago;

/** Pasarela de pagos en línea (Wompi Web Checkout u otra), en dos pasos: iniciar el cobro y consultar su resultado. */
public interface PasarelaPagoPort {

    InicioPago iniciarPago(SolicitudPago solicitud);

    ResultadoTransaccion consultarTransaccion(String idTransaccion);

    /**
     * Consulta el resultado del cobro por la referencia que se le dio al iniciarlo, sin necesitar el id de transacción.
     *
     * @param montoEnCentavos monto que el pago espera; la pasarela lo usa solo si no lo guarda ella misma.
     */
    ResultadoTransaccion consultarPorReferencia(String referencia, long montoEnCentavos);
}
