package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del historial de pagos con los datos de la cita, el cliente y el técnico. Sin datos sensibles de la pasarela.
 * El técnico, el monto final, el método, la fecha de pago y el comprobante son null mientras no existan.
 */
public record PagoDelHistorial(
        Long citaId,
        Long clienteId,
        String clienteNombre,
        Long tecnicoId,
        String tecnicoNombre,
        String especialidad,
        BigDecimal montoCotizado,
        BigDecimal montoFinal,
        MetodoPago metodo,
        EstadoPago estado,
        LocalDateTime pagadoEn,
        String numeroComprobante
) {
}
