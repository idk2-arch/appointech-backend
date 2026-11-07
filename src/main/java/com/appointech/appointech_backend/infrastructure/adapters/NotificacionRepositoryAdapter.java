package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.ports.out.NotificacionRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.NotificacionEntity;
import com.appointech.appointech_backend.infrastructure.repositories.NotificacionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificacionRepositoryAdapter implements NotificacionRepositoryPort {

    private final NotificacionJpaRepository notificacionJpaRepository;

    public NotificacionRepositoryAdapter(NotificacionJpaRepository notificacionJpaRepository) {
        this.notificacionJpaRepository = notificacionJpaRepository;
    }

    @Override
    public ListadoConTotal<Notificacion> listar(Long usuarioId, boolean soloNoLeidas, PaginacionOpcional paginacion) {
        PageRequest pagina = PageRequest.of(paginacion.pagina(), paginacion.tamanio());
        Page<NotificacionEntity> resultado = soloNoLeidas
                ? notificacionJpaRepository.findByUsuarioIdAndLeidaFalseOrderByCreadaEnDescIdDesc(usuarioId, pagina)
                : notificacionJpaRepository.findByUsuarioIdOrderByCreadaEnDescIdDesc(usuarioId, pagina);
        return new ListadoConTotal<>(resultado.getContent().stream().map(NotificacionRepositoryAdapter::aDominio).toList(),
                resultado.getTotalElements());
    }

    @Override
    public long contarNoLeidas(Long usuarioId) {
        return notificacionJpaRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    public Optional<Notificacion> marcarLeida(Long id, Long usuarioId) {
        return notificacionJpaRepository.findByIdAndUsuarioId(id, usuarioId).map(entity -> {
            entity.setLeida(true);
            return aDominio(notificacionJpaRepository.save(entity));
        });
    }

    @Override
    public int marcarTodasLeidas(Long usuarioId) {
        return notificacionJpaRepository.marcarTodasLeidas(usuarioId);
    }

    static Notificacion aDominio(NotificacionEntity entity) {
        return new Notificacion(entity.getId(), entity.getUsuarioId(), entity.getTipo(), entity.getTitulo(),
                entity.getMensaje(), entity.getCitaId(), entity.isLeida(), entity.getCreadaEn());
    }
}
