package com.appointech.appointech_backend.domain.models;

import java.time.LocalDate;

/** Rango pedido para las métricas; null en cualquiera de los dos extremos significa "el valor por defecto". */
public record CriteriosMetricas(LocalDate desde, LocalDate hasta) {
}
