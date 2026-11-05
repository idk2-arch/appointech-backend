package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarCostoFinalRequest(
        @NotNull(message = "El costo final es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo final debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El costo final admite hasta 10 enteros y 2 decimales")
        BigDecimal montoFinal
) {
}
