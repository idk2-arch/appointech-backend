package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Notificacion;

public interface MarcarNotificacionesLeidasUseCase {

    /** Marca una notificación propia como leída (idempotente); si no existe o es de otro usuario, falla con NotificacionNoEncontradaException. */
    Notificacion marcarLeida(String correo, Long notificacionId);

    /** Marca todas las propias como leídas y devuelve cuántas cambiaron. */
    int marcarTodasLeidas(String correo);
}
