package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado del pago de una cita. diferenciaSignificativa es true cuando el costo final se aparta del cotizado más que el
 * umbral configurado; porcentajeDiferencia es null mientras falte la cotización o el costo final. urlPago solo viene al
 * elegir el pago en línea.
 */
public record PagoResponse(
        Long citaId,
        BigDecimal montoCotizado,
        BigDecimal montoFinal,
        MetodoPago metodo,
        EstadoPago estado,
        String referencia,
        LocalDateTime pagadoEn,
        boolean diferenciaSignificativa,
        @Schema(description = "Diferencia absoluta entre el costo final y el cotizado, como porcentaje del cotizado "
                + "(87.5 significa 87,5 %; no es una fracción como 0.875). Es 0 o más y puede superar 100 si el costo "
                + "final es más del doble. Null mientras falte la cotización o el costo final.",
                example = "87.5", minimum = "0", nullable = true)
        BigDecimal porcentajeDiferencia,
        String urlPago
) {
}
