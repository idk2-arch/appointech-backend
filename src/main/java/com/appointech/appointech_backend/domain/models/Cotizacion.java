package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;

/** Valor estimado de una especialidad; sin monto cuando la especialidad no tiene costo de referencia. */
public record Cotizacion(Long especialidadId, String especialidad, BigDecimal montoEstimado, String nota) {

    public static final String NOTA_ESTIMADO = "Valor estimado; el costo final lo registra el técnico";
    public static final String NOTA_SIN_REFERENCIA = "Esta especialidad aún no tiene un valor de referencia; "
            + "el costo final lo registra el técnico al terminar el servicio";

    public static Cotizacion de(Especialidad especialidad) {
        BigDecimal costo = especialidad.getCostoReferencia();
        return new Cotizacion(especialidad.getId(), especialidad.getNombre(), costo,
                costo == null ? NOTA_SIN_REFERENCIA : NOTA_ESTIMADO);
    }

    public boolean disponible() {
        return montoEstimado != null;
    }
}
