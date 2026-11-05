package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;

public interface SeleccionarMetodoPagoUseCase {
    Pago seleccionar(Long citaId, MetodoPago metodo, String correoActor);
    Pago confirmarEfectivo(Long citaId, String correoActor);
}
