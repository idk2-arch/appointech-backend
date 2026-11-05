package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.models.SolicitudPago;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;

/**
 * Pasarela por defecto (PASARELA_PAGOS=wompi o sin definir, fail-closed) mientras no exista el cliente real de Wompi: la aplicación arranca y el pago
 * en línea responde "no disponible", sin afectar el pago en efectivo. No es un bean: lo crea PagosConfig. Se reemplaza
 * por el adaptador de Wompi.
 */
public class PasarelaPagoWompiPendienteAdapter implements PasarelaPagoPort {

    @Override
    public InicioPago iniciarPago(SolicitudPago solicitud) {
        throw new PasarelaNoDisponibleException();
    }

    @Override
    public ResultadoTransaccion consultarTransaccion(String idTransaccion) {
        throw new PasarelaNoDisponibleException();
    }

    @Override
    public ResultadoTransaccion consultarPorReferencia(String referencia, long montoEnCentavos) {
        throw new PasarelaNoDisponibleException();
    }
}
