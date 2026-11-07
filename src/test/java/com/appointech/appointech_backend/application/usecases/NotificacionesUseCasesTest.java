package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.NotificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.NotificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionesUseCasesTest {

    private static final String CORREO = "ana@x.com";
    private static final long USUARIO_ID = 11L;

    private UsuarioRepositoryPort usuarios;
    private NotificacionRepositoryPort repositorio;
    private ConsultarNotificacionesUseCaseImpl consultar;
    private MarcarNotificacionesLeidasUseCaseImpl marcar;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepositoryPort.class);
        repositorio = mock(NotificacionRepositoryPort.class);
        consultar = new ConsultarNotificacionesUseCaseImpl(usuarios, repositorio);
        marcar = new MarcarNotificacionesLeidasUseCaseImpl(usuarios, repositorio);
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
    }

    private static Notificacion notificacion(long id, boolean leida) {
        return new Notificacion(id, USUARIO_ID, TipoNotificacion.CITA_CONFIRMADA, "t", "m", 5L, leida,
                LocalDateTime.of(2030, 5, 2, 10, 0));
    }

    @Test
    void sinPaginacionDevuelveLaPrimeraPaginaDe20SoloDelUsuarioAutenticado() {
        ListadoConTotal<Notificacion> esperado = new ListadoConTotal<>(List.of(notificacion(1, false)), 41);
        when(repositorio.listar(USUARIO_ID, false, new PaginacionOpcional(0, 20))).thenReturn(esperado);

        assertEquals(esperado, consultar.listar(CORREO, false, PaginacionOpcional.de(null, null)));
    }

    @Test
    void conPaginacionYFiltroLosPasaTalCual() {
        ListadoConTotal<Notificacion> esperado = new ListadoConTotal<>(List.of(), 3);
        when(repositorio.listar(USUARIO_ID, true, new PaginacionOpcional(2, 5))).thenReturn(esperado);

        assertEquals(esperado, consultar.listar(CORREO, true, PaginacionOpcional.de(2, 5)));
    }

    @Test
    void unTamanioFueraDeRangoEsUnFiltroInvalido() {
        assertThrows(FiltroCitasInvalidoException.class, () -> PaginacionOpcional.de(0, 101));
        assertThrows(FiltroCitasInvalidoException.class, () -> PaginacionOpcional.de(0, 0));
    }

    @Test
    void cuentaLasNoLeidasDelUsuario() {
        when(repositorio.contarNoLeidas(USUARIO_ID)).thenReturn(4L);

        assertEquals(4L, consultar.contarNoLeidas(CORREO));
    }

    @Test
    void marcaUnaPropiaComoLeida() {
        Notificacion leida = notificacion(8, true);
        when(repositorio.marcarLeida(8L, USUARIO_ID)).thenReturn(Optional.of(leida));

        assertEquals(leida, marcar.marcarLeida(CORREO, 8L));
    }

    @Test
    void unaAjenaOInexistenteEsNotificacionNoEncontrada() {
        when(repositorio.marcarLeida(99L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(NotificacionNoEncontradaException.class, () -> marcar.marcarLeida(CORREO, 99L));
    }

    @Test
    void marcarTodasDevuelveCuantasCambiaron() {
        when(repositorio.marcarTodasLeidas(USUARIO_ID)).thenReturn(6);

        assertEquals(6, marcar.marcarTodasLeidas(CORREO));
    }

    @Test
    void conUnCorreoDesconocidoNadaSeConsultaNiSeMarca() {
        when(usuarios.buscarPorCorreo("nadie@x.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> consultar.listar("nadie@x.com", false, PaginacionOpcional.de(null, null)));
        assertThrows(UsuarioNoEncontradoException.class, () -> consultar.contarNoLeidas("nadie@x.com"));
        assertThrows(UsuarioNoEncontradoException.class, () -> marcar.marcarLeida("nadie@x.com", 1L));
        assertThrows(UsuarioNoEncontradoException.class, () -> marcar.marcarTodasLeidas("nadie@x.com"));
        verify(repositorio, never()).marcarTodasLeidas(org.mockito.ArgumentMatchers.anyLong());
    }
}
