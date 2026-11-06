package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;

public interface ConsultaHistorialPagosPort {

    /**
     * Una página de pagos con sus datos relacionados, en una sola consulta, y el total que cumple el filtro.
     * Orden: fecha de pago descendente (sin fecha al final) y id del pago descendente. La página y el tamaño del filtro son obligatorios.
     */
    ListadoConTotal<PagoDelHistorial> buscar(FiltroHistorialPagos filtro);
}
