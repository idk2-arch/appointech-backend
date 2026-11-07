package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.ports.in.ConsultarNotificacionesUseCase;
import com.appointech.appointech_backend.domain.ports.out.NotificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: la campana de la app. Cada usuario ve únicamente sus propias notificaciones. */
@Service
public class ConsultarNotificacionesUseCaseImpl implements ConsultarNotificacionesUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;

    public ConsultarNotificacionesUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                              NotificacionRepositoryPort notificacionRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.notificacionRepositoryPort = notificacionRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ListadoConTotal<Notificacion> listar(String correo, boolean soloNoLeidas, PaginacionOpcional paginacion) {
        PaginacionOpcional pedida = paginacion.paginado() ? paginacion
                : PaginacionOpcional.de(0, PaginacionOpcional.TAMANIO_POR_DEFECTO);
        return notificacionRepositoryPort.listar(usuarioId(correo), soloNoLeidas, pedida);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(String correo) {
        return notificacionRepositoryPort.contarNoLeidas(usuarioId(correo));
    }

    private Long usuarioId(String correo) {
        return usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo)).getId();
    }
}
