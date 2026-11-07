package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.DestinatariosAdminPort;
import com.appointech.appointech_backend.domain.ports.out.PublicadorNotificacionesPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvisosInternosTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2030-05-02T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 5, 2, 15, 0);
    private static final long USUARIO_CLIENTE = 101L;
    private static final long USUARIO_TECNICO = 202L;

    private PublicadorNotificacionesPort publicador;
    private DestinatariosAdminPort admins;
    private ClienteRepositoryPort clientes;
    private TecnicoRepositoryPort tecnicos;
    private CitaRepositoryPort citas;
    private AvisosInternos avisos;

    @BeforeEach
    void preparar() {
        publicador = mock(PublicadorNotificacionesPort.class);
        admins = mock(DestinatariosAdminPort.class);
        clientes = mock(ClienteRepositoryPort.class);
        tecnicos = mock(TecnicoRepositoryPort.class);
        citas = mock(CitaRepositoryPort.class);
        avisos = new AvisosInternos(publicador, admins, clientes, tecnicos, citas, RELOJ);

        Cliente cliente = mock(Cliente.class);
        when(cliente.getUsuario()).thenReturn(usuario(USUARIO_CLIENTE));
        when(clientes.buscarPorId(7L)).thenReturn(Optional.of(cliente));
        Tecnico tecnico = mock(Tecnico.class);
        when(tecnico.getUsuario()).thenReturn(usuario(USUARIO_TECNICO));
        when(tecnicos.buscarPorId(9L)).thenReturn(Optional.of(tecnico));
    }

    private static Usuario usuario(long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    private static Cita cita(Long tecnicoId, MotivoSolicitudEspecial motivo) {
        Cita cita = new Cita();
        cita.setId(55L);
        cita.setClienteId(7L);
        cita.setTecnicoId(tecnicoId);
        cita.setFechaHoraInicio(LocalDateTime.of(2030, 5, 6, 9, 30));
        cita.setMotivoSolicitudEspecial(motivo);
        return cita;
    }

    private List<Notificacion> publicadas(int cantidad) {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(publicador, times(cantidad)).publicar(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void solicitudNormalSoloAvisaAlClienteQueEligeUnaFranja() {
        avisos.solicitudRecibida(cita(null, null));

        Notificacion n = publicadas(1).get(0);
        assertEquals(USUARIO_CLIENTE, n.usuarioId());
        assertEquals(TipoNotificacion.SOLICITUD_RECIBIDA, n.tipo());
        assertEquals(55L, n.citaId());
        assertFalse(n.leida());
        assertEquals(AHORA, n.creadaEn());
        assertTrue(n.mensaje().contains("#55") && n.mensaje().contains("franja"));
        verifyNoInteractions(admins);
    }

    @Test
    void solicitudFueraDeCoberturaAvisaAlClienteYATodosLosAdministradores() {
        when(admins.idsAdministradoresActivos()).thenReturn(List.of(1L, 2L));

        avisos.solicitudRecibida(cita(null, MotivoSolicitudEspecial.FUERA_DE_COBERTURA));

        List<Notificacion> enviadas = publicadas(3);
        assertEquals(USUARIO_CLIENTE, enviadas.get(0).usuarioId());
        assertTrue(enviadas.get(0).mensaje().contains("administrador"));
        assertEquals(List.of(1L, 2L), List.of(enviadas.get(1).usuarioId(), enviadas.get(2).usuarioId()));
        assertEquals(TipoNotificacion.SOLICITUD_FUERA_DE_COBERTURA, enviadas.get(1).tipo());
        assertEquals("Solicitud fuera de cobertura", enviadas.get(1).titulo());
        assertEquals(55L, enviadas.get(1).citaId());
    }

    @Test
    void solicitudSinTecnicosAvisaALosAdministradoresConSuPropioTipo() {
        when(admins.idsAdministradoresActivos()).thenReturn(List.of(1L));

        avisos.solicitudRecibida(cita(null, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD));

        Notificacion admin = publicadas(2).get(1);
        assertEquals(TipoNotificacion.SOLICITUD_SIN_TECNICOS, admin.tipo());
        assertEquals("Solicitud sin técnicos", admin.titulo());
    }

    @Test
    void citaConfirmadaAvisaAlClienteYAlTecnicoConLaFecha() {
        avisos.citaConfirmada(cita(9L, null));

        List<Notificacion> enviadas = publicadas(2);
        assertEquals(TipoNotificacion.CITA_CONFIRMADA, enviadas.get(0).tipo());
        assertEquals(USUARIO_CLIENTE, enviadas.get(0).usuarioId());
        assertTrue(enviadas.get(0).mensaje().contains("06/05/2030 09:30"));
        assertEquals(TipoNotificacion.NUEVA_CITA_ASIGNADA, enviadas.get(1).tipo());
        assertEquals(USUARIO_TECNICO, enviadas.get(1).usuarioId());
        assertTrue(enviadas.get(1).mensaje().contains("#55"));
    }

    @Test
    void citaConfirmadaSinFechaNoInventaUnaYSinTecnicoNoAvisaANadieMas() {
        Cita sinFecha = cita(null, null);
        sinFecha.setFechaHoraInicio(null);

        avisos.citaConfirmada(sinFecha);

        Notificacion n = publicadas(1).get(0);
        assertEquals("Tu cita #55 quedó confirmada.", n.mensaje());
    }

    @Test
    void cancelacionDelClienteAvisaAAmbosYLaDelTecnicoSoloAlCliente() {
        avisos.citaCanceladaPorCliente(cita(9L, null));
        List<Notificacion> porCliente = publicadas(2);
        assertEquals(List.of(USUARIO_CLIENTE, USUARIO_TECNICO),
                List.of(porCliente.get(0).usuarioId(), porCliente.get(1).usuarioId()));
        assertEquals(TipoNotificacion.CITA_CANCELADA, porCliente.get(0).tipo());
        assertEquals(TipoNotificacion.CITA_CANCELADA, porCliente.get(1).tipo());

        org.mockito.Mockito.clearInvocations(publicador);
        avisos.citaCanceladaPorTecnico(cita(9L, null));
        Notificacion porTecnico = publicadas(1).get(0);
        assertEquals(USUARIO_CLIENTE, porTecnico.usuarioId());
        assertTrue(porTecnico.mensaje().startsWith("El técnico canceló"));
    }

    @Test
    void citaFinalizadaInvitaAlClienteACalificar() {
        avisos.citaFinalizada(cita(9L, null));

        Notificacion n = publicadas(1).get(0);
        assertEquals(TipoNotificacion.CITA_FINALIZADA, n.tipo());
        assertEquals("Tu cita #55 finalizó. Ya puedes calificar el servicio.", n.mensaje());
    }

    @Test
    void pagoRegistradoAvisaAlClienteDeLaCita() {
        Pago pago = new Pago();
        pago.setCitaId(55L);
        when(citas.buscarPorId(55L)).thenReturn(Optional.of(cita(9L, null)));

        avisos.pagoRegistrado(pago);

        Notificacion n = publicadas(1).get(0);
        assertEquals(TipoNotificacion.PAGO_REGISTRADO, n.tipo());
        assertEquals(USUARIO_CLIENTE, n.usuarioId());
    }

    @Test
    void pagoDeUnaCitaInexistenteNoAvisaANadie() {
        Pago pago = new Pago();
        pago.setCitaId(404L);
        when(citas.buscarPorId(404L)).thenReturn(Optional.empty());

        avisos.pagoRegistrado(pago);

        verify(publicador, never()).publicar(any());
    }

    @Test
    void sinClienteOTecnicoEncontradoNoSePublicaNada() {
        when(clientes.buscarPorId(7L)).thenReturn(Optional.empty());
        when(tecnicos.buscarPorId(9L)).thenReturn(Optional.empty());

        avisos.citaConfirmada(cita(9L, null));

        verify(publicador, never()).publicar(any());
    }

    @Test
    void siFallaLaBusquedaDelClienteElTecnicoIgualRecibeSuAviso() {
        when(clientes.buscarPorId(7L)).thenThrow(new IllegalStateException("base caída"));

        assertDoesNotThrow(() -> avisos.citaConfirmada(cita(9L, null)));

        Notificacion n = publicadas(1).get(0);
        assertEquals(USUARIO_TECNICO, n.usuarioId());
    }

    @Test
    void ningunFalloDelPublicadorOBusquedaDeAdministradoresRompeLaOperacion() {
        doThrow(new IllegalStateException("fallo")).when(publicador).publicar(any());
        when(admins.idsAdministradoresActivos()).thenThrow(new IllegalStateException("fallo"));
        when(citas.buscarPorId(55L)).thenThrow(new IllegalStateException("fallo"));
        Pago pago = new Pago();
        pago.setCitaId(55L);

        assertDoesNotThrow(() -> avisos.solicitudRecibida(cita(null, MotivoSolicitudEspecial.FUERA_DE_COBERTURA)));
        assertDoesNotThrow(() -> avisos.citaConfirmada(cita(9L, null)));
        assertDoesNotThrow(() -> avisos.citaCanceladaPorCliente(cita(9L, null)));
        assertDoesNotThrow(() -> avisos.citaCanceladaPorTecnico(cita(9L, null)));
        assertDoesNotThrow(() -> avisos.citaFinalizada(cita(9L, null)));
        assertDoesNotThrow(() -> avisos.pagoRegistrado(pago));
    }

    @Test
    void losMensajesNoLlevanCorreosNiTelefonos() {
        Cita solicitud = cita(9L, MotivoSolicitudEspecial.FUERA_DE_COBERTURA);
        solicitud.setTelefonoContacto("3001234567");
        when(admins.idsAdministradoresActivos()).thenReturn(List.of(1L));

        avisos.solicitudRecibida(solicitud);
        avisos.citaConfirmada(solicitud);

        for (Notificacion n : publicadas(4)) {
            assertFalse(n.mensaje().contains("@") || n.mensaje().contains("3001234567"), n.mensaje());
        }
    }
}
