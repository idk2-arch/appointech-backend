package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de una fila del historial de pagos (una sola consulta con joins). Solo los campos que el historial
 * expone: ninguna referencia ni identificador de la pasarela.
 */
public record PagoHistorialFila(
        Long citaId,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        Long tecnicoId,
        String tecnicoNombre,
        String tecnicoApellido,
        String especialidad,
        BigDecimal montoCotizado,
        BigDecimal montoFinal,
        MetodoPago metodo,
        EstadoPago estado,
        LocalDateTime pagadoEn,
        Long comprobanteId
) {
}
