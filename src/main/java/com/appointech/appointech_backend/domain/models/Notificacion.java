package com.appointech.appointech_backend.domain.models;

import java.time.LocalDateTime;

/** Un aviso interno para un usuario. Los textos no llevan correos ni teléfonos de nadie. */
public record Notificacion(Long id, Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
                           Long citaId, boolean leida, LocalDateTime creadaEn) {

    /** Una notificación recién creada: sin id y sin leer. */
    public static Notificacion nueva(Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
                                     Long citaId, LocalDateTime creadaEn) {
        return new Notificacion(null, usuarioId, tipo, titulo, mensaje, citaId, false, creadaEn);
    }
}
