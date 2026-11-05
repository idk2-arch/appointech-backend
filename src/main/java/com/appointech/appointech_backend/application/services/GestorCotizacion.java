package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.Pago;
import org.springframework.stereotype.Component;

/** Deja guardado en el pago el valor estimado que se le mostró al cliente, para compararlo con el costo final. */
@Component
public class GestorCotizacion {

    private final PagosDeCita pagos;

    public GestorCotizacion(PagosDeCita pagos) {
        this.pagos = pagos;
    }

    /**
     * Cotiza la cita; si la especialidad tiene costo de referencia lo guarda como monto cotizado del pago.
     * Sin costo de referencia no hay nada que guardar y la cotización se devuelve igual, sin error.
     */
    public Cotizacion registrar(Long citaId, Especialidad especialidad) {
        Cotizacion cotizacion = Cotizacion.de(especialidad);
        if (cotizacion.disponible()) {
            Pago pago = pagos.obtenerOCrearBloqueando(citaId);
            pago.registrarCotizacion(cotizacion.montoEstimado());
            pagos.guardar(pago, pago.getEstado(), null);
        }
        return cotizacion;
    }
}
