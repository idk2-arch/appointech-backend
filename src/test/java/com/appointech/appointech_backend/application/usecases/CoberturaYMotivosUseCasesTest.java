package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoberturaYMotivosUseCasesTest {

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    private FiltrarTecnicosPorEspecialidadUseCase filtrar;
    private CalculadoraDistanciaPort distancia;
    private CitaRepositoryPort citas;
    private BloqueIndisponibilidadRepositoryPort bloques;
    private ConfiguracionFranjasRepositoryPort configuracionPort;
    private CalcularFranjasDisponiblesUseCaseImpl calcular;

    @BeforeEach
    void preparar() {
        filtrar = mock(FiltrarTecnicosPorEspecialidadUseCase.class);
        distancia = mock(CalculadoraDistanciaPort.class);
        citas = mock(CitaRepositoryPort.class);
        bloques = mock(BloqueIndisponibilidadRepositoryPort.class);
        configuracionPort = mock(ConfiguracionFranjasRepositoryPort.class);
        when(configuracionPort.obtenerActual()).thenReturn(Optional.of(configuracion(15)));
        calcular = new CalcularFranjasDisponiblesUseCaseImpl(filtrar, citas, bloques, configuracionPort, distancia);
    }

    // ---------- motivos ----------

    @Test
    void sinTecnicosConLaEspecialidadDaSinTecnicosEspecialidad() {
        when(filtrar.filtrar(5L)).thenReturn(List.of());

        ResultadoFranjas r = calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals(MotivoSinFranjas.SIN_TECNICOS_ESPECIALIDAD, r.motivo());
        assertEquals(List.of(), r.franjas());
        verify(distancia, never()).calcular(any(), any());
    }

    @Test
    void tecnicosQueNoLleganDentroDelLimiteDanFueraDeCobertura() {
        when(filtrar.filtrar(5L)).thenReturn(List.of(tecnico(1L, 100)));
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(40.0, 60.0)));

        ResultadoFranjas r = calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals(MotivoSinFranjas.FUERA_DE_COBERTURA, r.motivo());
        assertEquals(List.of(), r.franjas());
    }

    @Test
    void tecnicosSinUbicacionBaseDanFueraDeCobertura() {
        Tecnico sinBase = tecnico(1L, 10);
        sinBase.setLatitudBase(null);
        sinBase.setLongitudBase(null);
        when(filtrar.filtrar(5L)).thenReturn(List.of(sinBase));

        assertEquals(MotivoSinFranjas.FUERA_DE_COBERTURA,
                calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638).motivo());
    }

    @Test
    void conCoberturaPeroSinHorariosLibresDaSinHorarios() {
        when(filtrar.filtrar(5L)).thenReturn(List.of(tecnico(1L, 10)));
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(3.0, 5.0)));
        // Todo el día ocupado por una cita.
        var cita = new com.appointech.appointech_backend.domain.models.Cita();
        cita.setId(9L);
        cita.setFechaHoraInicio(MANANA.atStartOfDay());
        cita.setFechaHoraFin(MANANA.plusDays(1).atStartOfDay());
        when(citas.buscarActivasDelTecnicoEntre(anyLong(), any(), any())).thenReturn(List.of(cita));

        ResultadoFranjas r = calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals(MotivoSinFranjas.SIN_HORARIOS, r.motivo());
        assertEquals(List.of(), r.franjas());
    }

    @Test
    void conFranjasElMotivoEsNulo() {
        when(filtrar.filtrar(5L)).thenReturn(List.of(tecnico(1L, 10)));
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(3.0, 5.0)));

        ResultadoFranjas r = calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertFalse(r.franjas().isEmpty());
        assertNull(r.motivo());
    }

    // ---------- límite único ----------

    @Test
    void elRadioDelTecnicoYaNoSeUsaSoloElMaximoDelSistema() {
        // Radio propio de 2 km, pero está a 10 km y el máximo del sistema es 15: se ofrece.
        when(filtrar.filtrar(5L)).thenReturn(List.of(tecnico(1L, 2)));
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(10.0, 15.0)));

        assertFalse(calcular.calcular(5L, MANANA, 4.3365, -74.3638).isEmpty());
    }

    @Test
    void unTecnicoConRadioPropioMayorNoSuperaElMaximoDelSistema() {
        when(configuracionPort.obtenerActual()).thenReturn(Optional.of(configuracion(8)));
        when(filtrar.filtrar(5L)).thenReturn(List.of(tecnico(1L, 100)));
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(10.0, 15.0)));

        assertEquals(MotivoSinFranjas.FUERA_DE_COBERTURA,
                calcular.calcularConMotivo(5L, MANANA, 4.3365, -74.3638).motivo());
    }

    // ---------- configuración del límite ----------

    @Test
    void obtieneElRadioActual() {
        assertEquals(15, new CoberturaUseCasesImpl(configuracionPort).obtener().radioMaximoKm());
    }

    @Test
    void sinConfiguracionGuardadaSeUsaElValorInicialDe15() {
        when(configuracionPort.obtenerActual()).thenReturn(Optional.empty());

        assertEquals(15, new CoberturaUseCasesImpl(configuracionPort).obtener().radioMaximoKm());
    }

    @Test
    void actualizaElRadioDentroDelRangoYLoGuarda() {
        when(configuracionPort.guardar(any(ConfiguracionFranjas.class))).thenAnswer(i -> i.getArgument(0));
        var sut = new CoberturaUseCasesImpl(configuracionPort);

        assertEquals(30, sut.actualizar(30, null).radioMaximoKm());
        assertEquals(1, sut.actualizar(1, null).radioMaximoKm());
        assertEquals(100, sut.actualizar(100, null).radioMaximoKm());
    }

    @Test
    void rechazaRadiosFueraDeRangoConMensajeClaroYNoGuarda() {
        var sut = new CoberturaUseCasesImpl(configuracionPort);

        for (Integer invalido : new Integer[]{null, 0, -5, 101, 1000}) {
            RadioCoberturaInvalidoException ex = assertThrows(RadioCoberturaInvalidoException.class,
                    () -> sut.actualizar(invalido, null));
            assertEquals("El radio máximo de cobertura debe ser mayor que 0 y como máximo 100 km", ex.getMessage());
        }
        verify(configuracionPort, never()).guardar(any());
    }

    // ---------- helpers ----------

    private static ConfiguracionFranjas configuracion(int radio) {
        return new ConfiguracionFranjas(1L, 120, radio, LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    private static Tecnico tecnico(Long id, int radioPropio) {
        Usuario u = new Usuario();
        u.setId(id + 100);
        u.setNombre("Ana");
        u.setApellido("Ruiz");
        u.setActivo(true);
        Tecnico t = new Tecnico();
        t.setId(id);
        t.setUsuario(u);
        t.setRadioCoberturaKm(radioPropio);
        Coordenadas base = new Coordenadas(4.40, -74.30);
        t.setLatitudBase(base.latitud());
        t.setLongitudBase(base.longitud());
        return t;
    }
}
