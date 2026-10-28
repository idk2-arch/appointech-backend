package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearSolicitudServicioFlujoTest {

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private EspecialidadRepositoryPort especialidades;
    @Mock
    private CitaRepositoryPort citas;
    @Mock
    private RegistroLogPort logs;
    @InjectMocks
    private CrearSolicitudServicioUseCaseImpl useCase;

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(5L);
        return u;
    }

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(15L);
        return c;
    }

    private Especialidad especialidad(boolean activa) {
        Especialidad e = new Especialidad();
        e.setId(2L);
        e.setActivo(activa);
        return e;
    }

    @Test
    void creaSolicitudPendienteSinTecnicoYRegistraAuditoria() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario()));
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.of(cliente()));
        when(especialidades.buscarPorId(2L)).thenReturn(Optional.of(especialidad(true)));
        when(citas.guardar(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(77L);
            return c;
        });

        Cita r = useCase.crear("ana@mail.com", 2L, "fuga", "Calle 1", 4.6, -74.1);

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citas).guardar(captor.capture());
        Cita guardada = captor.getValue();
        assertEquals(EstadoCita.PENDIENTE, guardada.getEstado());
        assertEquals(15L, guardada.getClienteId());
        assertEquals(2L, guardada.getEspecialidadId());
        assertNull(guardada.getTecnicoId());
        assertEquals("fuga", guardada.getDescripcionProblema());
        assertEquals("Calle 1", guardada.getDireccionServicio());
        assertEquals(4.6, guardada.getLatitud());
        assertEquals(-74.1, guardada.getLongitud());
        assertNotNull(guardada.getCreadaEn());
        assertEquals(guardada.getCreadaEn(), guardada.getActualizadaEn());
        assertEquals(77L, r.getId());
        verify(logs).registrar(eq(NivelLog.INFO), eq(TipoLog.NEGOCIO), eq("CrearSolicitudServicioUseCaseImpl"),
                eq("ana@mail.com"), eq("Solicitud de servicio creada con id 77"));
    }

    @Test
    void coordenadasNulasSeRechazanAntesDeConsultarNada() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> useCase.crear("ana@mail.com", 2L, "fuga", "Calle 1", null, -74.1));

        verify(usuarios, never()).buscarPorCorreo(any());
    }

    @Test
    void correoSinUsuarioLanzaClienteNoEncontrado() {
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class,
                () -> useCase.crear("x@mail.com", 2L, "fuga", "Calle 1", 4.6, -74.1));

        verify(citas, never()).guardar(any());
    }

    @Test
    void usuarioSinFilaDeClienteLanzaClienteNoEncontrado() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario()));
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class,
                () -> useCase.crear("ana@mail.com", 2L, "fuga", "Calle 1", 4.6, -74.1));
    }

    @Test
    void especialidadInexistenteOInactivaSeRechaza() {
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(usuario()));
        when(clientes.buscarPorUsuarioId(5L)).thenReturn(Optional.of(cliente()));
        when(especialidades.buscarPorId(2L)).thenReturn(Optional.of(especialidad(false)));
        when(especialidades.buscarPorId(3L)).thenReturn(Optional.empty());

        assertThrows(EspecialidadNoEncontradaException.class,
                () -> useCase.crear("ana@mail.com", 2L, "fuga", "Calle 1", 4.6, -74.1));
        assertThrows(EspecialidadNoEncontradaException.class,
                () -> useCase.crear("ana@mail.com", 3L, "fuga", "Calle 1", 4.6, -74.1));

        verify(citas, never()).guardar(any());
        verify(logs, never()).registrar(any(), any(), any(), any(), any());
    }
}
