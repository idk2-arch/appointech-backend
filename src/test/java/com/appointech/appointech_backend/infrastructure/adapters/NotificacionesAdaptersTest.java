package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.infrastructure.entities.NotificacionEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.NotificacionJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.UsuarioJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionesAdaptersTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 5, 2, 10, 0);

    private final NotificacionJpaRepository jpa = mock(NotificacionJpaRepository.class);

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static NotificacionEntity entidad(long id, boolean leida) {
        return new NotificacionEntity(id, 11L, TipoNotificacion.PAGO_REGISTRADO, "t", "m", 5L, leida, FECHA);
    }

    // ---------- lectura ----------

    @Test
    void listarTodasUsaElOrdenMasRecientePrimeroYDevuelveElTotal() {
        NotificacionRepositoryAdapter adapter = new NotificacionRepositoryAdapter(jpa);
        when(jpa.findByUsuarioIdOrderByCreadaEnDescIdDesc(11L, PageRequest.of(1, 2)))
                .thenReturn(new PageImpl<>(List.of(entidad(3, false), entidad(2, true)), PageRequest.of(1, 2), 7));

        ListadoConTotal<Notificacion> listado = adapter.listar(11L, false, new PaginacionOpcional(1, 2));

        assertEquals(7, listado.total());
        assertEquals(List.of(3L, 2L), listado.elementos().stream().map(Notificacion::id).toList());
        assertEquals(new Notificacion(3L, 11L, TipoNotificacion.PAGO_REGISTRADO, "t", "m", 5L, false, FECHA),
                listado.elementos().get(0));
        verify(jpa, never()).findByUsuarioIdAndLeidaFalseOrderByCreadaEnDescIdDesc(any(), any());
    }

    @Test
    void listarSoloNoLeidasUsaLaConsultaFiltrada() {
        NotificacionRepositoryAdapter adapter = new NotificacionRepositoryAdapter(jpa);
        when(jpa.findByUsuarioIdAndLeidaFalseOrderByCreadaEnDescIdDesc(11L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entidad(4, false))));

        ListadoConTotal<Notificacion> listado = adapter.listar(11L, true, new PaginacionOpcional(0, 20));

        assertEquals(1, listado.total());
        assertFalse(listado.elementos().get(0).leida());
        verify(jpa, never()).findByUsuarioIdOrderByCreadaEnDescIdDesc(any(), any());
    }

    @Test
    void contarYMarcarTodasDelegan() {
        NotificacionRepositoryAdapter adapter = new NotificacionRepositoryAdapter(jpa);
        when(jpa.countByUsuarioIdAndLeidaFalse(11L)).thenReturn(3L);
        when(jpa.marcarTodasLeidas(11L)).thenReturn(3);

        assertEquals(3L, adapter.contarNoLeidas(11L));
        assertEquals(3, adapter.marcarTodasLeidas(11L));
    }

    @Test
    void marcarLeidaBuscaSoloEntreLasDelUsuarioYLaGuardaLeida() {
        NotificacionRepositoryAdapter adapter = new NotificacionRepositoryAdapter(jpa);
        NotificacionEntity sinLeer = entidad(8, false);
        when(jpa.findByIdAndUsuarioId(8L, 11L)).thenReturn(Optional.of(sinLeer));
        when(jpa.save(sinLeer)).thenReturn(sinLeer);

        Optional<Notificacion> resultado = adapter.marcarLeida(8L, 11L);

        assertTrue(resultado.orElseThrow().leida());
        assertTrue(sinLeer.isLeida());
    }

    @Test
    void marcarLeidaDeUnaAjenaDevuelveVacioSinGuardar() {
        NotificacionRepositoryAdapter adapter = new NotificacionRepositoryAdapter(jpa);
        when(jpa.findByIdAndUsuarioId(8L, 99L)).thenReturn(Optional.empty());

        assertTrue(adapter.marcarLeida(8L, 99L).isEmpty());
        verify(jpa, never()).save(any());
    }

    // ---------- publicación ----------

    private PublicadorNotificacionesAdapter publicador() {
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        return new PublicadorNotificacionesAdapter(jpa, tm);
    }

    private static Notificacion nueva() {
        return Notificacion.nueva(11L, TipoNotificacion.CITA_FINALIZADA, "Servicio finalizado", "mensaje", 5L, FECHA);
    }

    @Test
    void sinTransaccionEnCursoGuardaDeInmediatoConTodosSusCampos() {
        publicador().publicar(nueva());

        ArgumentCaptor<NotificacionEntity> captor = ArgumentCaptor.forClass(NotificacionEntity.class);
        verify(jpa).save(captor.capture());
        NotificacionEntity guardada = captor.getValue();
        assertEquals(11L, guardada.getUsuarioId());
        assertEquals(TipoNotificacion.CITA_FINALIZADA, guardada.getTipo());
        assertEquals("Servicio finalizado", guardada.getTitulo());
        assertEquals("mensaje", guardada.getMensaje());
        assertEquals(5L, guardada.getCitaId());
        assertFalse(guardada.isLeida());
        assertEquals(FECHA, guardada.getCreadaEn());
    }

    @Test
    void conTransaccionEnCursoEsperaAQueLaOperacionPrincipalTermineBien() {
        TransactionSynchronizationManager.initSynchronization();

        publicador().publicar(nueva());

        verify(jpa, never()).save(any());
        List<TransactionSynchronization> pendientes = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, pendientes.size());
        pendientes.forEach(TransactionSynchronization::afterCommit);
        verify(jpa).save(any(NotificacionEntity.class));
    }

    @Test
    void siLaOperacionPrincipalSeRevierteNoQuedaNingunAviso() {
        TransactionSynchronizationManager.initSynchronization();

        publicador().publicar(nueva());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(jpa, never()).save(any());
    }

    @Test
    void siGuardarFallaNoSePropagaNiAntesNiDespuesDelCommit() {
        when(jpa.save(any())).thenThrow(new IllegalStateException("base caída"));
        PublicadorNotificacionesAdapter adapter = publicador();

        assertDoesNotThrow(() -> adapter.publicar(nueva()));

        TransactionSynchronizationManager.initSynchronization();
        adapter.publicar(nueva());
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
    }

    @Test
    void guardaEnUnaTransaccionPropiaQueNoComparteLaDeLaOperacionPrincipal() {
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        new PublicadorNotificacionesAdapter(jpa, tm).publicar(nueva());

        ArgumentCaptor<TransactionDefinition> definicion = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(tm).getTransaction(definicion.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definicion.getValue().getPropagationBehavior());
    }

    // ---------- administradores ----------

    @Test
    void losDestinatariosAdminSonLosUsuariosActivosConRolAdministrador() {
        UsuarioJpaRepository usuarios = mock(UsuarioJpaRepository.class);
        when(usuarios.idsActivosPorRol(RolUsuarioEntity.ADMINISTRADOR)).thenReturn(List.of(1L, 4L));

        assertEquals(List.of(1L, 4L), new DestinatariosAdminAdapter(usuarios).idsAdministradoresActivos());
    }
}
