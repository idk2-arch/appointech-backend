package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;

/**
 * Parámetros del módulo de pagos.
 *
 * @param umbralDiferenciaPorcentaje diferencia (en %) entre el monto cotizado y el final a partir de la cual se avisa.
 * @param urlRetorno                 página del cliente a la que la pasarela devuelve al usuario tras pagar.
 */
public record ConfiguracionPagos(BigDecimal umbralDiferenciaPorcentaje, String urlRetorno) {

    public ConfiguracionPagos {
        if (umbralDiferenciaPorcentaje == null || umbralDiferenciaPorcentaje.signum() < 0) {
            throw new IllegalArgumentException("El umbral de diferencia no puede ser negativo");
        }
    }

    /** URL de retorno de una cita: la base configurada más ?citaId=, respetando parámetros y fragmento que ya traiga. */
    public String urlRetornoPara(Long citaId) {
        int ancla = urlRetorno.indexOf('#');
        String base = ancla < 0 ? urlRetorno : urlRetorno.substring(0, ancla);
        String fragmento = ancla < 0 ? "" : urlRetorno.substring(ancla);
        String separador;
        if (base.indexOf('?') < 0) {
            separador = "?";
        } else if (base.endsWith("?") || base.endsWith("&")) {
            separador = "";
        } else {
            separador = "&";
        }
        return base + separador + "citaId=" + citaId + fragmento;
    }
}
