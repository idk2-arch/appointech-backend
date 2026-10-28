package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.HistorialEstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.HistorialEstadoCitaJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialYCitasSprint4AdaptersTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 27, 9, 0);

    @Mock
    private HistorialEstadoCitaJpaRepository historialJpa;
    @Mock
    private CitaJpaRepository citaJpa;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ClienteRepositoryPort clientes;

    // ---- HistorialEstadoCitaAdapter ----

    @Test
    void registrarConvierteDominioAEntidadYDevuelveLaFilaGuardada() {
        when(historialJpa.save(any(HistorialEstadoCitaEntity.class))).thenAnswer(inv -> {
            HistorialEstadoCitaEntity e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        HistorialEstadoCita r = new HistorialEstadoCitaAdapter(historialJpa).registrar(
                new HistorialEstadoCita(null, 10L, EstadoCita.CONFIRMADA, EstadoCita.CANCELADA, FECHA, "ana@mail.com", "Ya no"));

        ArgumentCaptor<HistorialEstadoCitaEntity> captor = ArgumentCaptor.forClass(HistorialEstadoCitaEntity.class);
        verify(historialJpa).save(captor.capture());
        HistorialEstadoCitaEntity guardada = captor.getValue();
        assertEquals(10L, guardada.getCitaId());
        assertEquals(EstadoCitaEntity.CONFIRMADA, guardada.getEstadoAnterior());
        assertEquals(EstadoCitaEntity.CANCELADA, guardada.getEstadoNuevo());
        assertEquals(FECHA, guardada.getFecha());
        assertEquals("ana@mail.com", guardada.getActor());
        assertEquals("Ya no", guardada.getMotivo());
        assertEquals(7L, r.id());
        assertEquals(EstadoCita.CANCELADA, r.estadoNuevo());
    }

    @Test
    void registrarAceptaUnEstadoAnteriorNulo() {
        when(historialJpa.save(any(HistorialEstadoCitaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        HistorialEstadoCita r = new HistorialEstadoCitaAdapter(historialJpa).registrar(
                new HistorialEstadoCita(null, 10L, null, EstadoCita.PENDIENTE, FECHA, "ana@mail.com", null));

        assertNull(r.estadoAnterior());
        assertNull(r.motivo());
        assertEquals(EstadoCita.PENDIENTE, r.estadoNuevo());
    }

    @Test
    void listarPorCitaDevuelveLasFilasEnElOrdenDelRepositorio() {
        HistorialEstadoCitaEntity a = new HistorialEstadoCitaEntity(1L, 10L, EstadoCitaEntity.CONFIRMADA,
                EstadoCitaEntity.EN_CAMINO, FECHA, "tec@mail.com", null);
        HistorialEstadoCitaEntity b = new HistorialEstadoCitaEntity(2L, 10L, EstadoCitaEntity.EN_CAMINO,
                EstadoCitaEntity.EN_PROCESO, FECHA.plusHours(1), "tec@mail.com", null);
        when(historialJpa.findByCitaIdOrderByFechaAscIdAsc(10L)).thenReturn(List.of(a, b));

        List<HistorialEstadoCita> r = new HistorialEstadoCitaAdapter(historialJpa).listarPorCita(10L);

        assertEquals(2, r.size());
        assertEquals(EstadoCita.EN_CAMINO, r.get(0).estadoNuevo());
        assertEquals(EstadoCita.EN_PROCESO, r.get(1).estadoNuevo());
        assertEquals(FECHA.plusHours(1), r.get(1).fecha());
    }

    // ---- CitaRepositoryAdapter: listado por cliente y campos del diagnóstico ----

    @Test
    void listarPorClienteConvierteLasEntidadesYConservaElOrden() {
        CitaEntity reciente = new CitaEntity();
        reciente.setId(2L);
        reciente.setEstado(EstadoCitaEntity.PENDIENTE);
        CitaEntity vieja = new CitaEntity();
        vieja.setId(1L);
        vieja.setEstado(EstadoCitaEntity.FINALIZADA);
        when(citaJpa.findByClienteIdOrderByCreadaEnDescIdDesc(4L)).thenReturn(List.of(reciente, vieja));

        List<Cita> r = new CitaRepositoryAdapter(citaJpa).listarPorCliente(4L);

        assertEquals(List.of(2L, 1L), r.stream().map(Cita::getId).toList());
        assertEquals(EstadoCita.FINALIZADA, r.get(1).getEstado());
    }

    @Test
    void guardarYLeerConservanDiagnosticoRecomendacionesYFecha() {
        Cita c = new Cita();
        c.setId(5L);
        c.setEstado(EstadoCita.EN_PROCESO);
        c.setDiagnostico("Sifón roto");
        c.setRecomendaciones("Cambiarlo");
        c.setFechaDiagnostico(FECHA);
        when(citaJpa.save(any(CitaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita r = new CitaRepositoryAdapter(citaJpa).guardar(c);

        ArgumentCaptor<CitaEntity> captor = ArgumentCaptor.forClass(CitaEntity.class);
        verify(citaJpa).save(captor.capture());
        assertEquals("Cambiarlo", captor.getValue().getRecomendaciones());
        assertEquals(FECHA, captor.getValue().getFechaDiagnostico());
        assertEquals("Sifón roto", r.getDiagnostico());
        assertEquals("Cambiarlo", r.getRecomendaciones());
        assertEquals(FECHA, r.getFechaDiagnostico());
    }

    // ---- ResolverClienteAutenticado ----

    @Test
    void resolverClienteTraduceElCorreoDelTokenAlCliente() {
        Usuario u = new Usuario();
        u.setId(3L);
        Cliente c = new Cliente();
        c.setId(9L);
        when(usuarios.buscarPorCorreo("ana@mail.com")).thenReturn(Optional.of(u));
        when(clientes.buscarPorUsuarioId(3L)).thenReturn(Optional.of(c));

        assertSame(c, new ResolverClienteAutenticado(usuarios, clientes).resolver("ana@mail.com"));
    }

    @Test
    void resolverClienteFallaSiNoExisteElUsuarioOSuPerfilDeCliente() {
        Usuario u = new Usuario();
        u.setId(3L);
        when(usuarios.buscarPorCorreo("x@mail.com")).thenReturn(Optional.empty());
        when(usuarios.buscarPorCorreo("tec@mail.com")).thenReturn(Optional.of(u));
        when(clientes.buscarPorUsuarioId(3L)).thenReturn(Optional.empty());
        ResolverClienteAutenticado resolver = new ResolverClienteAutenticado(usuarios, clientes);

        assertThrows(ClienteNoEncontradoException.class, () -> resolver.resolver("x@mail.com"));
        assertThrows(ClienteNoEncontradoException.class, () -> resolver.resolver("tec@mail.com"));
        verifyNoInteractions(historialJpa);
    }
}
