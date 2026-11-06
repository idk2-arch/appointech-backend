package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;

public interface ConsultarHistorialPagosUseCase {

    /**
     * Historial de pagos filtrado, del más reciente al más antiguo (los que no tienen fecha de pago, al final).
     * Siempre paginado: sin página ni tamaño devuelve la primera página de 20.
     *
     * @throws com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException si la página, el tamaño o el rango de fechas no son válidos
     */
    ListadoConTotal<PagoDelHistorial> consultar(FiltroHistorialPagos filtro);
}
