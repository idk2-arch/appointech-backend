package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasAsignacionManual;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TecnicoDisponible;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarTecnicosDisponiblesUseCaseImplTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);
    private static final LocalDateTime FIN = INICIO.plusHours(2);

    private CitaRepositoryPort citaRepo;
    private BloqueIndisponibilidadRepositoryPort bloquesRepo;
    private FiltrarTecnicosPorEspecialidadUseCase filtrar;
    private CalculadoraDistanciaPort distancia;
    private ConsultarTecnicosDisponiblesUseCaseImpl casoDeUso;

    @BeforeEach
    void preparar() {
        citaRepo = mock(CitaRepositoryPort.class);
        bloquesRepo = mock(BloqueIndisponibilidadRepositoryPort.class);
        filtrar = mock(FiltrarTecnicosPorEspecialidadUseCase.class);
        distancia = mock(CalculadoraDistanciaPort.class);
        casoDeUso = new ConsultarTecnicosDisponiblesUseCaseImpl(
                new ReglasAsignacionManual(citaRepo, bloquesRepo), filtrar, distancia);
        when(citaRepo.buscarPorId(15L)).thenReturn(Optional.of(solicitud()));
    }

    private static Cita solicitud() {
        Cita c = new Cita();
        c.setId(15L);
        c.setClienteId(2L);
        c.setEspecialidadId(9L);
        c.setLatitud(4.60);
        c.setLongitud(-74.08);
        c.setEstado(EstadoCita.PENDIENTE);
        c.setAgendamientoManual(true);
        return c;
    }

    private static Tecnico tecnico(long id, String nombre, Double latitud, Double longitud) {
        Usuario u = new Usuario();
        u.setId(100 + id);
        u.setNombre(nombre);
        u.setApellido("Apellido" + id);
        u.setTelefono("30" + id);
        u.setActivo(true);
        u.setFotoClave(id == 1 ? "foto-1" : null);
        Tecnico t = new Tecnico();
        t.setId(id);
        t.setUsuario(u);
        t.setLatitudBase(latitud);
        t.setLongitudBase(longitud);
        return t;
    }

    @Test
    void ordenaPorDistanciaAscendenteYDejaAlFinalALosQueNoTienenUbicacion() {
        Tecnico lejano = tecnico(1, "Lejano", 4.9, -74.0);
        Tecnico sinUbicacion = tecnico(2, "SinUbicacion", null, null);
        Tecnico cercano = tecnico(3, "Cercano", 4.61, -74.08);
        when(filtrar.filtrar(9L)).thenReturn(List.of(lejano, sinUbicacion, cercano));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(
                List.of(new Distancia(30.0, 40.0), new Distancia(1.2, 3.0)));

        List<TecnicoDisponible> r = casoDeUso.consultar(15L, INICIO, FIN);

        assertEquals(List.of(3L, 1L, 2L), r.stream().map(TecnicoDisponible::tecnicoId).toList());
        assertEquals(1.2, r.get(0).distanciaKm());
        assertEquals(30.0, r.get(1).distanciaKm());
        assertNull(r.get(2).distanciaKm());
        assertEquals("Cercano", r.get(0).nombre());
        assertEquals("Apellido3", r.get(0).apellido());
        assertEquals("303", r.get(0).telefono());
        assertEquals("foto-1", r.get(1).fotoClave());
    }

    @Test
    void conIgualDistanciaDesempataPorIdDelTecnico() {
        when(filtrar.filtrar(9L)).thenReturn(List.of(tecnico(8, "B", 4.6, -74.0), tecnico(7, "A", 4.6, -74.1)));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(
                List.of(new Distancia(5.0, 6.0), new Distancia(5.0, 6.0)));

        assertEquals(List.of(7L, 8L),
                casoDeUso.consultar(15L, INICIO, FIN).stream().map(TecnicoDisponible::tecnicoId).toList());
    }

    @Test
    void excluyeALosTecnicosConUnaCitaQueSeCruzaPeroNoALosQueSoloColindan() {
        Tecnico ocupado = tecnico(1, "Ocupado", 4.6, -74.0);
        Tecnico colindante = tecnico(2, "Colindante", 4.6, -74.1);
        when(filtrar.filtrar(9L)).thenReturn(List.of(ocupado, colindante));
        Cita cruzada = new Cita();
        cruzada.setId(50L);
        cruzada.setFechaHoraInicio(INICIO.plusHours(1));
        cruzada.setFechaHoraFin(FIN.plusHours(1));
        Cita queTermina = new Cita();
        queTermina.setId(51L);
        queTermina.setFechaHoraInicio(INICIO.minusHours(2));
        queTermina.setFechaHoraFin(INICIO);
        when(citaRepo.buscarActivasDelTecnicoEntre(1L, INICIO, FIN)).thenReturn(List.of(cruzada));
        when(citaRepo.buscarActivasDelTecnicoEntre(2L, INICIO, FIN)).thenReturn(List.of(queTermina));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(List.of(new Distancia(2.0, 3.0)));

        List<TecnicoDisponible> r = casoDeUso.consultar(15L, INICIO, FIN);

        assertEquals(List.of(2L), r.stream().map(TecnicoDisponible::tecnicoId).toList());
    }

    @Test
    void excluyeALosTecnicosConUnBloqueoQueSeCruza() {
        when(filtrar.filtrar(9L)).thenReturn(List.of(tecnico(1, "Bloqueado", 4.6, -74.0)));
        BloqueIndisponibilidad bloque = mock(BloqueIndisponibilidad.class);
        when(bloque.getInicio()).thenReturn(INICIO.minusHours(1));
        when(bloque.getFin()).thenReturn(INICIO.plusMinutes(30));
        when(bloquesRepo.buscarDelTecnicoEntre(1L, INICIO, FIN)).thenReturn(List.of(bloque));

        assertTrue(casoDeUso.consultar(15L, INICIO, FIN).isEmpty());
        verify(distancia, never()).calcular(any(), any());
    }

    @Test
    void laPropiaSolicitudNoCuentaComoOcupacion() {
        Tecnico t = tecnico(1, "Libre", 4.6, -74.0);
        when(filtrar.filtrar(9L)).thenReturn(List.of(t));
        Cita propia = solicitud();
        propia.setFechaHoraInicio(INICIO);
        propia.setFechaHoraFin(FIN);
        when(citaRepo.buscarActivasDelTecnicoEntre(1L, INICIO, FIN)).thenReturn(List.of(propia));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(List.of(new Distancia(2.0, 3.0)));

        assertEquals(1, casoDeUso.consultar(15L, INICIO, FIN).size());
    }

    @Test
    void sinTecnicosConLaEspecialidadDevuelveListaVacia() {
        when(filtrar.filtrar(9L)).thenReturn(List.of());

        assertTrue(casoDeUso.consultar(15L, INICIO, FIN).isEmpty());
    }

    @Test
    void siLaSolicitudNoTieneUbicacionTodosVanSinDistanciaOrdenadosPorId() {
        Cita sinUbicacion = solicitud();
        sinUbicacion.setLatitud(null);
        sinUbicacion.setLongitud(null);
        when(citaRepo.buscarPorId(15L)).thenReturn(Optional.of(sinUbicacion));
        when(filtrar.filtrar(9L)).thenReturn(List.of(tecnico(2, "B", 4.6, -74.0), tecnico(1, "A", 4.6, -74.1)));

        List<TecnicoDisponible> r = casoDeUso.consultar(15L, INICIO, FIN);

        assertEquals(List.of(1L, 2L), r.stream().map(TecnicoDisponible::tecnicoId).toList());
        assertTrue(r.stream().allMatch(t -> t.distanciaKm() == null));
        verify(distancia, never()).calcular(any(), any());
    }

    @Test
    void unaUbicacionInvalidaDeLaBaseDelTecnicoLoDejaSinDistancia() {
        Tecnico baseInvalida = tecnico(1, "BaseInvalida", 60.0, 10.0);
        Tecnico valida = tecnico(2, "Valida", 4.6, -74.0);
        when(filtrar.filtrar(9L)).thenReturn(List.of(baseInvalida, valida));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(List.of(new Distancia(2.0, 3.0)));

        List<TecnicoDisponible> r = casoDeUso.consultar(15L, INICIO, FIN);

        assertEquals(List.of(2L, 1L), r.stream().map(TecnicoDisponible::tecnicoId).toList());
        assertNull(r.get(1).distanciaKm());
    }

    @Test
    void siNingunTecnicoTieneBaseNoSeLlamaAlCalculadorDeDistancias() {
        when(filtrar.filtrar(9L)).thenReturn(List.of(tecnico(1, "A", null, null)));

        assertNull(casoDeUso.consultar(15L, INICIO, FIN).get(0).distanciaKm());
        verify(distancia, never()).calcular(any(), any());
    }

    @Test
    void siElCalculadorDevuelveOtraCantidadFallaConElErrorInterno() {
        when(filtrar.filtrar(9L)).thenReturn(List.of(tecnico(1, "A", 4.6, -74.0)));
        when(distancia.calcular(any(Coordenadas.class), any())).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> casoDeUso.consultar(15L, INICIO, FIN));
    }

    @Test
    void finNoPosteriorAlInicioEsInvalidoYNoConsultaNada() {
        assertThrows(FranjaInvalidaException.class, () -> casoDeUso.consultar(15L, FIN, INICIO));
        assertThrows(FranjaInvalidaException.class, () -> casoDeUso.consultar(15L, INICIO, INICIO));
        verify(citaRepo, never()).buscarPorId(any());
    }

    @Test
    void solicitudInexistenteOQueNoEsManualEs404() {
        when(citaRepo.buscarPorId(99L)).thenReturn(Optional.empty());
        Cita normal = solicitud();
        normal.setId(16L);
        normal.setAgendamientoManual(false);
        when(citaRepo.buscarPorId(16L)).thenReturn(Optional.of(normal));

        assertThrows(SolicitudNoEncontradaException.class, () -> casoDeUso.consultar(99L, INICIO, FIN));
        assertThrows(SolicitudNoEncontradaException.class, () -> casoDeUso.consultar(16L, INICIO, FIN));
    }

    @Test
    void solicitudYaAsignadaEs409() {
        Cita asignada = solicitud();
        asignada.setEstado(EstadoCita.CONFIRMADA);
        asignada.setTecnicoId(3L);
        when(citaRepo.buscarPorId(15L)).thenReturn(Optional.of(asignada));

        assertThrows(FranjaNoDisponibleException.class, () -> casoDeUso.consultar(15L, INICIO, FIN));
        verify(filtrar, never()).filtrar(any());
    }
}
