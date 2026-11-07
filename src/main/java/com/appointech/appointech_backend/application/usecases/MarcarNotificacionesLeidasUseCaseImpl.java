package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.NotificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.ports.in.MarcarNotificacionesLeidasUseCase;
import com.appointech.appointech_backend.domain.ports.out.NotificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: marcar como leídas las notificaciones propias; las de otros usuarios no existen para quien las pide. */
@Service
public class MarcarNotificacionesLeidasUseCaseImpl implements MarcarNotificacionesLeidasUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;

    public MarcarNotificacionesLeidasUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                                 NotificacionRepositoryPort notificacionRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.notificacionRepositoryPort = notificacionRepositoryPort;
    }

    @Override
    @Transactional
    public Notificacion marcarLeida(String correo, Long notificacionId) {
        return notificacionRepositoryPort.marcarLeida(notificacionId, usuarioId(correo))
                .orElseThrow(() -> new NotificacionNoEncontradaException(notificacionId));
    }

    @Override
    @Transactional
    public int marcarTodasLeidas(String correo) {
        return notificacionRepositoryPort.marcarTodasLeidas(usuarioId(correo));
    }

    private Long usuarioId(String correo) {
        return usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo)).getId();
    }
}
