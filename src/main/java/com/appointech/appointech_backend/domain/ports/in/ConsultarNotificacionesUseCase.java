package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;

public interface ConsultarNotificacionesUseCase {

    /** Las notificaciones del usuario autenticado, más recientes primero; sin paginación explícita, la primera página de 20. */
    ListadoConTotal<Notificacion> listar(String correo, boolean soloNoLeidas, PaginacionOpcional paginacion);

    long contarNoLeidas(String correo);
}
