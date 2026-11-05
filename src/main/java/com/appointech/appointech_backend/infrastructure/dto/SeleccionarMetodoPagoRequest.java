package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.MetodoPago;
import jakarta.validation.constraints.NotNull;

public record SeleccionarMetodoPagoRequest(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodo
) {
}
