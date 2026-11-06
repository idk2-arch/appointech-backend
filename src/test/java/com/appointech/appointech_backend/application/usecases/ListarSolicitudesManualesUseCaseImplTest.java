package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarSolicitudesManualesUseCaseImplTest {

    private CitaRepositoryPort citaRepo;
    private ConsultaClientesPort consulta;
    private EspecialidadRepositoryPort especialidadRepo;
    private ListarSolicitudesManualesUseCaseImpl casoDeUso;

    @BeforeEach
    void preparar() {
        citaRepo = mock(CitaRepositoryPort.class);
        consulta = mock(ConsultaClientesPort.class);
        especialidadRepo = mock(EspecialidadRepositoryPort.class);
        casoDeUso = new ListarSolicitudesManualesUseCaseImpl(citaRepo, consulta, especialidadRepo);
    }

    private static Cita solicitud(long id, EstadoCita estado, MotivoSolicitudEspecial motivo) {
        Cita c = new Cita();
        c.setId(id);
        c.setClienteId(2L);
        c.setEspecialidadId(5L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Vereda");
        c.setLatitud(4.5);
        c.setLongitud(-74.1);
        c.setEstado(estado);
        c.setAgendamientoManual(true);
        c.setMotivoSolicitudEspecial(motivo);
        c.setTelefonoContacto("3109998877");
        c.setCreadaEn(LocalDateTime.of(2030, 5, 1, 8, 0));
        return c;
    }

    private void datosDeApoyo() {
        when(consulta.resumenesPorId(any())).thenReturn(Map.of(2L, new ClienteResumen("Ana", "Ruiz", "3001112233")));
        Especialidad e = new Especialidad();
        e.setId(5L);
        e.setNombre("Plomería");
        when(especialidadRepo.buscarPorIds(any())).thenReturn(List.of(e));
    }

    private FiltroSolicitudesManuales filtroEnviadoAlPuerto() {
        ArgumentCaptor<FiltroSolicitudesManuales> captor = ArgumentCaptor.forClass(FiltroSolicitudesManuales.class);
        verify(citaRepo).buscarSolicitudesManuales(captor.capture());
        return captor.getValue();
    }

    private void puertoDevuelve(List<Cita> citas, long total) {
        when(citaRepo.buscarSolicitudesManuales(any())).thenReturn(new ListadoConTotal<>(citas, total));
    }

    @Test
    void listaLasSolicitudesConDatosDelClienteLaEspecialidadYElEstado() {
        puertoDevuelve(List.of(solicitud(70L, EstadoCita.PENDIENTE, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD)), 1);
        datosDeApoyo();

        ListadoConTotal<SolicitudManualDetalle> r = casoDeUso.listar(FiltroSolicitudesManuales.sinFiltros());

        assertEquals(1, r.total());
        SolicitudManualDetalle d = r.elementos().get(0);
        assertEquals("Ana", d.clienteNombre());
        assertEquals("Ruiz", d.clienteApellido());
        assertEquals("3109998877", d.telefonoContacto());
        assertEquals("Vereda", d.direccionServicio());
        assertEquals("Fuga", d.descripcionProblema());
        assertEquals("Plomería", d.especialidadNombre());
        assertEquals(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, d.motivo());
        assertEquals(EstadoCita.PENDIENTE, d.estado());
    }

    @Test
    void sinTelefonoDeContactoUsaElDeLaCuentaDelCliente() {
        Cita c = solicitud(70L, EstadoCita.PENDIENTE, null);
        c.setTelefonoContacto(null);
        puertoDevuelve(List.of(c), 1);
        datosDeApoyo();

        assertEquals("3001112233", casoDeUso.listar().get(0).telefonoContacto());
    }

    @Test
    void sinDatosDelClienteNiDeLaEspecialidadDejaEsosCamposEnNulo() {
        Cita c = solicitud(70L, EstadoCita.PENDIENTE, null);
        c.setTelefonoContacto(null);
        puertoDevuelve(List.of(c), 1);
        when(consulta.resumenesPorId(any())).thenReturn(Map.of());
        when(especialidadRepo.buscarPorIds(any())).thenReturn(List.of());

        SolicitudManualDetalle d = casoDeUso.listar().get(0);

        assertNull(d.clienteNombre());
        assertNull(d.clienteApellido());
        assertNull(d.telefonoContacto());
        assertNull(d.especialidadNombre());
    }

    @Test
    void sinFiltroDeEstadoBuscaLasPendientes() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(FiltroSolicitudesManuales.sinFiltros());

        assertEquals(EstadoCita.PENDIENTE, filtroEnviadoAlPuerto().estado());
    }

    @Test
    void respetaElEstadoPedido() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(new FiltroSolicitudesManuales(null, null, EstadoCita.CONFIRMADA, null, null));

        assertEquals(EstadoCita.CONFIRMADA, filtroEnviadoAlPuerto().estado());
    }

    @Test
    void pasaElMotivoYLaEspecialidadAlPuertoCombinados() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(new FiltroSolicitudesManuales(
                MotivoSolicitudEspecial.FUERA_DE_COBERTURA, 5L, EstadoCita.PENDIENTE, null, null));

        FiltroSolicitudesManuales f = filtroEnviadoAlPuerto();
        assertEquals(MotivoSolicitudEspecial.FUERA_DE_COBERTURA, f.motivo());
        assertEquals(5L, f.especialidadId());
    }

    @Test
    void sinPageNiSizeNoPagina() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(FiltroSolicitudesManuales.sinFiltros());

        FiltroSolicitudesManuales f = filtroEnviadoAlPuerto();
        assertFalse(f.paginado());
        assertNull(f.pagina());
        assertNull(f.tamanio());
    }

    @Test
    void conPageYSizePidePaginadoAlPuertoYDevuelveElTotalCompleto() {
        puertoDevuelve(List.of(solicitud(70L, EstadoCita.PENDIENTE, null)), 37);
        datosDeApoyo();

        ListadoConTotal<SolicitudManualDetalle> r =
                casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, 3, 10));

        FiltroSolicitudesManuales f = filtroEnviadoAlPuerto();
        assertTrue(f.paginado());
        assertEquals(3, f.pagina());
        assertEquals(10, f.tamanio());
        assertEquals(1, r.elementos().size());
        assertEquals(37, r.total());
    }

    @Test
    void paginaVaciaDevuelveListaVaciaPeroConElTotalYNoConsultaClientes() {
        puertoDevuelve(List.of(), 12);

        ListadoConTotal<SolicitudManualDetalle> r =
                casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, 9, 10));

        assertTrue(r.elementos().isEmpty());
        assertEquals(12, r.total());
        verify(consulta, never()).resumenesPorId(any());
        verify(especialidadRepo, never()).buscarPorIds(any());
    }

    @Test
    void soloPageUsaElTamanioPorDefecto() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, 2, null));

        FiltroSolicitudesManuales f = filtroEnviadoAlPuerto();
        assertEquals(2, f.pagina());
        assertEquals(ListarSolicitudesManualesUseCaseImpl.TAMANIO_POR_DEFECTO, f.tamanio());
    }

    @Test
    void soloSizeUsaLaPrimeraPagina() {
        puertoDevuelve(List.of(), 0);

        casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, null, 5));

        FiltroSolicitudesManuales f = filtroEnviadoAlPuerto();
        assertEquals(0, f.pagina());
        assertEquals(5, f.tamanio());
    }

    @Test
    void aceptaLosLimitesDelTamanio() {
        puertoDevuelve(List.of(), 0);

        assertTrue(casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, 0, 1)).elementos().isEmpty());
        assertTrue(casoDeUso.listar(new FiltroSolicitudesManuales(null, null, null, 0,
                ListarSolicitudesManualesUseCaseImpl.TAMANIO_MAXIMO)).elementos().isEmpty());
    }

    @Test
    void rechazaPaginaNegativaTamanioCeroOMayorAlMaximo() {
        FiltroSolicitudesManuales paginaNegativa = new FiltroSolicitudesManuales(null, null, null, -1, 10);
        FiltroSolicitudesManuales tamanioCero = new FiltroSolicitudesManuales(null, null, null, 0, 0);
        FiltroSolicitudesManuales tamanioExcesivo = new FiltroSolicitudesManuales(null, null, null, 0,
                ListarSolicitudesManualesUseCaseImpl.TAMANIO_MAXIMO + 1);

        assertThrows(FiltroCitasInvalidoException.class, () -> casoDeUso.listar(paginaNegativa));
        assertThrows(FiltroCitasInvalidoException.class, () -> casoDeUso.listar(tamanioCero));
        assertThrows(FiltroCitasInvalidoException.class, () -> casoDeUso.listar(tamanioExcesivo));
        verify(citaRepo, never()).buscarSolicitudesManuales(any());
    }

    @Test
    void elListadoSinFiltrosDevuelveSoloLosElementosEnElOrdenDelPuerto() {
        puertoDevuelve(List.of(solicitud(1L, EstadoCita.PENDIENTE, null), solicitud(2L, EstadoCita.PENDIENTE, null)), 2);
        datosDeApoyo();

        List<SolicitudManualDetalle> lista = casoDeUso.listar();

        assertEquals(List.of(1L, 2L), lista.stream().map(SolicitudManualDetalle::id).toList());
    }

    @Test
    void sinSolicitudesNoConsultaClientesNiEspecialidades() {
        puertoDevuelve(List.of(), 0);

        assertTrue(casoDeUso.listar().isEmpty());
        verify(consulta, never()).resumenesPorId(any());
    }
}
