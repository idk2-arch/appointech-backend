package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Notificacion;

/**
 * Crea una notificación interna sin afectar la operación que la origina: si hay una transacción en curso se guarda
 * cuando ésta termina bien, y un fallo al guardar solo se registra, nunca se propaga.
 */
public interface PublicadorNotificacionesPort {
    void publicar(Notificacion notificacion);
}
