package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.ports.out.PublicadorNotificacionesPort;
import com.appointech.appointech_backend.infrastructure.entities.NotificacionEntity;
import com.appointech.appointech_backend.infrastructure.repositories.NotificacionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Guarda la notificación cuando la transacción de la operación principal ya terminó bien (si no hay ninguna, de
 * inmediato) y en una transacción propia: así un fallo de guardado nunca marca para revertir la operación principal
 * y una operación revertida no deja avisos.
 */
@Component
public class PublicadorNotificacionesAdapter implements PublicadorNotificacionesPort {

    private static final Logger log = LoggerFactory.getLogger(PublicadorNotificacionesAdapter.class);

    private final NotificacionJpaRepository notificacionJpaRepository;
    private final TransactionTemplate transaccionPropia;

    public PublicadorNotificacionesAdapter(NotificacionJpaRepository notificacionJpaRepository,
                                           PlatformTransactionManager transactionManager) {
        this.notificacionJpaRepository = notificacionJpaRepository;
        this.transaccionPropia = new TransactionTemplate(transactionManager);
        this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void publicar(Notificacion notificacion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    guardar(notificacion);
                }
            });
        } else {
            guardar(notificacion);
        }
    }

    private void guardar(Notificacion notificacion) {
        try {
            transaccionPropia.executeWithoutResult(estado -> notificacionJpaRepository.save(aEntidad(notificacion)));
        } catch (RuntimeException ex) {
            log.warn("No se pudo guardar la notificación {} del usuario {}: {}", notificacion.tipo(),
                    notificacion.usuarioId(), ex.getClass().getSimpleName());
        }
    }

    private static NotificacionEntity aEntidad(Notificacion notificacion) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.setUsuarioId(notificacion.usuarioId());
        entity.setTipo(notificacion.tipo());
        entity.setTitulo(notificacion.titulo());
        entity.setMensaje(notificacion.mensaje());
        entity.setCitaId(notificacion.citaId());
        entity.setLeida(false);
        entity.setCreadaEn(notificacion.creadaEn());
        return entity;
    }
}
