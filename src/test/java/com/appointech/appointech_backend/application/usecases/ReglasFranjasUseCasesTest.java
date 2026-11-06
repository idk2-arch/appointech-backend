package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.exception.ReglasFranjasInvalidasException;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.ReglasFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoActualizacionReglas;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReglasFranjasUseCasesTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime SEIS_PM = LocalTime.of(18, 0);
    private static final String ADMIN = "admin@appointech.com";

    private ConfiguracionFranjasRepositoryPort puerto;
    private RegistroLogPort log;
    private ReglasFranjasUseCasesImpl casoDeUso;

    @BeforeEach
    void preparar() {
        puerto = mock(ConfiguracionFranjasRepositoryPort.class);
        log = mock(RegistroLogPort.class);
        casoDeUso = new ReglasFranjasUseCasesImpl(puerto, log);
        when(puerto.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void obtieneLasReglasGuardadas() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(
                new ConfiguracionFranjas(1L, 90, 40, LocalTime.of(7, 30), LocalTime.of(17, 0))));

        assertEquals(new ReglasFranjas(40, 90, LocalTime.of(7, 30), LocalTime.of(17, 0)), casoDeUso.obtener());
    }

    @Test
    void sinConfiguracionGuardadaUsaElRespaldoUnico() {
        when(puerto.obtenerActual()).thenReturn(Optional.empty());

        assertEquals(new ReglasFranjas(15, 120, OCHO, SEIS_PM), casoDeUso.obtener());
    }

    @Test
    void actualizaYGuardaLasReglasRegistrandoAlAdministrador() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(
                new ConfiguracionFranjas(1L, 120, 15, OCHO, SEIS_PM)));

        ResultadoActualizacionReglas r = casoDeUso.actualizar(ADMIN, 30, 60, LocalTime.of(7, 0), LocalTime.of(19, 0));

        assertEquals(new ReglasFranjas(30, 60, LocalTime.of(7, 0), LocalTime.of(19, 0)), r.reglas());
        ArgumentCaptor<ConfiguracionFranjas> guardada = ArgumentCaptor.forClass(ConfiguracionFranjas.class);
        verify(puerto).guardar(guardada.capture());
        assertEquals(1L, guardada.getValue().getId());
        assertEquals(60, guardada.getValue().getDuracionBloqueMinutos());
        verify(log).registrar(eq(NivelLog.INFO), eq(TipoLog.AUDITORIA), eq("ReglasFranjasUseCasesImpl"),
                eq(ADMIN), any());
    }

    @Test
    void avisaCuandoCambiarDuracionOJornadaInvalidaFranjasOfrecidas() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(
                new ConfiguracionFranjas(1L, 120, 15, OCHO, SEIS_PM)));

        assertTrue(casoDeUso.actualizar(ADMIN, 15, 60, OCHO, SEIS_PM).franjasOfrecidasInvalidadas());
        assertTrue(casoDeUso.actualizar(ADMIN, 15, 120, LocalTime.of(9, 0), SEIS_PM).franjasOfrecidasInvalidadas());
    }

    @Test
    void cambiarSoloElRadioNoInvalidaFranjasOfrecidas() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(
                new ConfiguracionFranjas(1L, 120, 15, OCHO, SEIS_PM)));

        ResultadoActualizacionReglas r = casoDeUso.actualizar(ADMIN, 50, 120, OCHO, SEIS_PM);

        assertFalse(r.franjasOfrecidasInvalidadas());
        assertEquals(50, r.reglas().radioMaximoKm());
    }

    @Test
    void sinConfiguracionGuardadaActualizaSobreElRespaldo() {
        when(puerto.obtenerActual()).thenReturn(Optional.empty());

        ResultadoActualizacionReglas r = casoDeUso.actualizar(ADMIN, 20, 120, OCHO, SEIS_PM);

        assertFalse(r.franjasOfrecidasInvalidadas());
        assertEquals(20, r.reglas().radioMaximoKm());
    }

    @Test
    void radioInvalidoSeRechazaYNoSeGuardaNada() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(ConfiguracionFranjas.porDefecto()));

        assertThrows(RadioCoberturaInvalidoException.class,
                () -> casoDeUso.actualizar(ADMIN, -5, 60, OCHO, SEIS_PM));
        assertThrows(RadioCoberturaInvalidoException.class,
                () -> casoDeUso.actualizar(ADMIN, 101, 60, OCHO, SEIS_PM));

        verify(puerto, never()).guardar(any());
        verify(log, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void duracionOJornadaInvalidasSeRechazanYNoSeGuardaNada() {
        when(puerto.obtenerActual()).thenReturn(Optional.of(ConfiguracionFranjas.porDefecto()));

        assertThrows(ReglasFranjasInvalidasException.class,
                () -> casoDeUso.actualizar(ADMIN, 15, -60, OCHO, SEIS_PM));
        assertThrows(ReglasFranjasInvalidasException.class,
                () -> casoDeUso.actualizar(ADMIN, 15, 60, SEIS_PM, OCHO));

        verify(puerto, never()).guardar(any());
        verify(log, never()).registrar(any(), any(), any(), any(), any());
    }
}
