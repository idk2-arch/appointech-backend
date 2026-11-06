package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.ReglasFranjasInvalidasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasDeFranjasDominioTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime SEIS_PM = LocalTime.of(18, 0);

    @Test
    void elRespaldoPorDefectoEsUnicoYConocido() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        assertNull(c.getId());
        assertEquals(120, c.getDuracionBloqueMinutos());
        assertEquals(15, c.getRadioMaximoKm());
        assertEquals(OCHO, c.getHoraInicioJornada());
        assertEquals(SEIS_PM, c.getHoraFinJornada());
    }

    @Test
    void cambiaDuracionYJornadaJuntas() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        c.cambiarReglasDeFranjas(60, LocalTime.of(7, 30), LocalTime.of(17, 0));

        assertEquals(60, c.getDuracionBloqueMinutos());
        assertEquals(LocalTime.of(7, 30), c.getHoraInicioJornada());
        assertEquals(LocalTime.of(17, 0), c.getHoraFinJornada());
    }

    @ParameterizedTest
    @ValueSource(ints = {-30, -1, 0, 14, 481, 10_000})
    void rechazaDuracionesNegativasOAbsurdas(int duracion) {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        ReglasFranjasInvalidasException ex = assertThrows(ReglasFranjasInvalidasException.class,
                () -> c.cambiarReglasDeFranjas(duracion, OCHO, SEIS_PM));

        assertEquals("La duración del bloque debe estar entre 15 y 480 minutos", ex.getMessage());
        assertEquals(120, c.getDuracionBloqueMinutos());
    }

    @Test
    void aceptaLosLimitesDeDuracion() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        c.cambiarReglasDeFranjas(15, OCHO, SEIS_PM);
        assertEquals(15, c.getDuracionBloqueMinutos());

        c.cambiarReglasDeFranjas(480, OCHO, SEIS_PM);
        assertEquals(480, c.getDuracionBloqueMinutos());
    }

    @Test
    void rechazaDuracionNula() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        assertThrows(ReglasFranjasInvalidasException.class, () -> c.cambiarReglasDeFranjas(null, OCHO, SEIS_PM));
    }

    @Test
    void rechazaJornadaIncompleta() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        assertThrows(ReglasFranjasInvalidasException.class, () -> c.cambiarReglasDeFranjas(60, null, SEIS_PM));
        assertThrows(ReglasFranjasInvalidasException.class, () -> c.cambiarReglasDeFranjas(60, OCHO, null));
    }

    @Test
    void rechazaInicioIgualOPosteriorAlFin() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        ReglasFranjasInvalidasException igual = assertThrows(ReglasFranjasInvalidasException.class,
                () -> c.cambiarReglasDeFranjas(60, OCHO, OCHO));
        assertEquals("La hora de inicio de la jornada debe ser anterior a la de fin", igual.getMessage());
        assertThrows(ReglasFranjasInvalidasException.class,
                () -> c.cambiarReglasDeFranjas(60, SEIS_PM, OCHO));
        assertEquals(OCHO, c.getHoraInicioJornada());
        assertEquals(SEIS_PM, c.getHoraFinJornada());
    }

    @Test
    void rechazaUnaJornadaMasCortaQueElBloque() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        ReglasFranjasInvalidasException ex = assertThrows(ReglasFranjasInvalidasException.class,
                () -> c.cambiarReglasDeFranjas(120, OCHO, LocalTime.of(9, 0)));

        assertEquals("La jornada debe poder contener al menos un bloque completo", ex.getMessage());
        assertEquals(SEIS_PM, c.getHoraFinJornada());
    }

    @Test
    void detectaSiElCambioInvalidaFranjasOfrecidas() {
        ConfiguracionFranjas c = ConfiguracionFranjas.porDefecto();

        assertFalse(c.cambiaranFranjasOfrecidas(120, OCHO, SEIS_PM));
        assertTrue(c.cambiaranFranjasOfrecidas(60, OCHO, SEIS_PM));
        assertTrue(c.cambiaranFranjasOfrecidas(120, LocalTime.of(9, 0), SEIS_PM));
        assertTrue(c.cambiaranFranjasOfrecidas(120, OCHO, LocalTime.of(17, 0)));
    }

    @Test
    void reglasFranjasSeArmaDesdeLaConfiguracion() {
        ReglasFranjas r = ReglasFranjas.de(ConfiguracionFranjas.porDefecto());

        assertEquals(new ReglasFranjas(15, 120, OCHO, SEIS_PM), r);
    }
}
