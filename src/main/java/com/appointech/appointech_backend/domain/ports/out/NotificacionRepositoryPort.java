package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;

import java.util.Optional;

/** Consulta y lectura de las notificaciones de un usuario; ninguna operación toca las de otros. */
public interface NotificacionRepositoryPort {

    /** Las más recientes primero; la paginación debe venir completa. El total es el de todas las que cumplen el filtro. */
    ListadoConTotal<Notificacion> listar(Long usuarioId, boolean soloNoLeidas, PaginacionOpcional paginacion);

    long contarNoLeidas(Long usuarioId);

    /** La marca como leída (si ya lo estaba, no cambia nada); vacío si no existe o es de otro usuario. */
    Optional<Notificacion> marcarLeida(Long id, Long usuarioId);

    /** Marca como leídas todas las del usuario y devuelve cuántas cambiaron. */
    int marcarTodasLeidas(Long usuarioId);
}
