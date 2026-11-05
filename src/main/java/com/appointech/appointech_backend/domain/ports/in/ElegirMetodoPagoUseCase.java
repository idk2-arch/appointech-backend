package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.ResultadoMetodoPago;

public interface ElegirMetodoPagoUseCase {
    /** El cliente dueño elige EFECTIVO o EN_LINEA; en línea devuelve además la URL de pago. */
    ResultadoMetodoPago elegir(String correoCliente, Long citaId, MetodoPago metodo);
}
