package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;

public record Cotizacion(Long citaId, String especialidad, BigDecimal montoEstimado) {
}
