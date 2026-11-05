package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Genera el comprobante en el momento en que el pago queda PAGADO; nunca hay dos por pago. */
@Component
public class EmisorComprobante {

    private final ComprobantePagoRepositoryPort comprobanteRepositoryPort;

    public EmisorComprobante(ComprobantePagoRepositoryPort comprobanteRepositoryPort) {
        this.comprobanteRepositoryPort = comprobanteRepositoryPort;
    }

    /** Emite el comprobante si el pago está PAGADO y aún no tiene uno; si ya existe, no hace nada. */
    public void emitirSiPagado(Pago pago, LocalDateTime ahora) {
        if (pago.getEstado() != EstadoPago.PAGADO) {
            return;
        }
        if (comprobanteRepositoryPort.buscarPorPagoId(pago.getId()).isEmpty()) {
            comprobanteRepositoryPort.guardar(new ComprobantePago(null, pago.getId(), ahora));
        }
    }
}
