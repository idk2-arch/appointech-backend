package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante con los datos legibles de la cita ya resueltos (nombres de cliente y técnico, especialidad). */
public record ComprobanteDetalle(String numero, LocalDateTime emitidoEn, Long citaId, String cliente,
                                 String tecnico, String especialidad, BigDecimal monto, MetodoPago metodo,
                                 String referencia, LocalDateTime pagadoEn) {
}
