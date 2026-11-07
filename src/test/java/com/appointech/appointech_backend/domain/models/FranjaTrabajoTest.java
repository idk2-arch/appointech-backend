package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.FranjaTrabajoInvalidaException;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FranjaTrabajoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 5, 2, 10, 0);
    private static final LocalDate MANANA = LocalDate.of(2030, 5, 3);

    private static FranjaTrabajo validada(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return FranjaTrabajo.validada(null, 1L, fecha, inicio, fin, 120, AHORA, 9L, AHORA);
    }

    private static FranjaTrabajo franja(int inicio, int fin) {
        return new FranjaTrabajo(1L, 1L, MANANA, LocalTime.of(inicio, 0), LocalTime.of(fin, 0), 9L, AHORA);
    }

    private static String mensaje(Runnable accion) {
        return assertThrows(FranjaTrabajoInvalidaException.class, accion::run).getMessage();
    }

    @Test
    void unaFranjaValidaConservaSusDatos() {
        FranjaTrabajo f = validada(MANANA, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertEquals(MANANA, f.fecha());
        assertEquals(LocalDateTime.of(2030, 5, 3, 8, 0), f.inicio());
        assertEquals(LocalDateTime.of(2030, 5, 3, 12, 0), f.fin());
        assertEquals("2030-05-03 de 08:00 a 12:00", f.descripcion());
    }

    @Test
    void todosLosDatosSonObligatorios() {
        String esperado = "La fecha, la hora de inicio y la hora de fin son obligatorias";
        assertEquals(esperado, mensaje(() -> validada(null, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        assertEquals(esperado, mensaje(() -> validada(MANANA, null, LocalTime.of(12, 0))));
        assertEquals(esperado, mensaje(() -> validada(MANANA, LocalTime.of(8, 0), null)));
    }

    @Test
    void elInicioDebeSerAnteriorAlFin() {
        assertEquals("La hora de inicio debe ser anterior a la hora de fin",
                mensaje(() -> validada(MANANA, LocalTime.of(12, 0), LocalTime.of(8, 0))));
        assertEquals("La hora de inicio debe ser anterior a la hora de fin",
                mensaje(() -> validada(MANANA, LocalTime.of(8, 0), LocalTime.of(8, 0))));
    }

    @Test
    void debeCaberAlMenosUnaCitaDeLaDuracionGlobal() {
        assertEquals("La franja debe durar al menos 120 minutos, que es lo que dura una cita",
                mensaje(() -> validada(MANANA, LocalTime.of(8, 0), LocalTime.of(9, 59))));
        validada(MANANA, LocalTime.of(8, 0), LocalTime.of(10, 0));
    }

    @Test
    void laFechaNoPuedeSerPasadaYSiEsHoyLaHoraNoPuedeHaberPasado() {
        assertEquals("La fecha de la franja no puede ser anterior a hoy",
                mensaje(() -> validada(LocalDate.of(2030, 5, 1), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        assertEquals("La hora de inicio no puede ser anterior a la hora actual",
                mensaje(() -> validada(LocalDate.of(2030, 5, 2), LocalTime.of(9, 59), LocalTime.of(14, 0))));
        validada(LocalDate.of(2030, 5, 2), LocalTime.of(10, 0), LocalTime.of(14, 0));
        validada(LocalDate.of(2030, 5, 2), LocalTime.of(11, 0), LocalTime.of(14, 0));
    }

    @Test
    void dosFranjasSeCruzanSoloSiComparteAlgunMinutoEnLaMismaFecha() {
        assertTrue(franja(8, 12).seCruzaCon(franja(10, 14)));
        assertTrue(franja(10, 14).seCruzaCon(franja(8, 12)));
        assertTrue(franja(8, 18).seCruzaCon(franja(10, 12)));
        assertFalse(franja(8, 12).seCruzaCon(franja(12, 16)));
        assertFalse(franja(12, 16).seCruzaCon(franja(8, 12)));
        FranjaTrabajo otroDia = new FranjaTrabajo(2L, 1L, MANANA.plusDays(1), LocalTime.of(8, 0), LocalTime.of(12, 0), 9L, AHORA);
        assertFalse(franja(8, 12).seCruzaCon(otroDia));
    }

    @Test
    void divideLaVentanaEnCitasDesdeLaHoraDeInicioYDescartaElSobrante() {
        List<FranjaTrabajo.Bloque> exacto = franja(8, 12).bloques(120);
        assertEquals(2, exacto.size());
        assertEquals(LocalDateTime.of(2030, 5, 3, 10, 0), exacto.get(1).inicio());
        assertEquals(LocalDateTime.of(2030, 5, 3, 12, 0), exacto.get(1).fin());

        assertEquals(1, franja(8, 11).bloques(120).size());
        assertEquals(0, franja(8, 9).bloques(120).size());
        assertEquals(5, franja(8, 18).bloques(120).size());
        assertEquals(10, franja(8, 18).bloques(60).size());
    }

    @Test
    void soloOfreceExactamenteSusBloques() {
        FranjaTrabajo f = franja(8, 12);

        assertTrue(f.ofreceBloque(LocalDateTime.of(2030, 5, 3, 8, 0), LocalDateTime.of(2030, 5, 3, 10, 0), 120));
        assertTrue(f.ofreceBloque(LocalDateTime.of(2030, 5, 3, 10, 0), LocalDateTime.of(2030, 5, 3, 12, 0), 120));
        assertFalse(f.ofreceBloque(LocalDateTime.of(2030, 5, 3, 9, 0), LocalDateTime.of(2030, 5, 3, 11, 0), 120));
        assertFalse(f.ofreceBloque(LocalDateTime.of(2030, 5, 3, 12, 0), LocalDateTime.of(2030, 5, 3, 14, 0), 120));
    }

    @Test
    void elRadioDeAtencionDelTecnicoVaDe1a100Km() {
        Tecnico tecnico = new Tecnico();

        tecnico.cambiarRadioCobertura(1);
        assertEquals(1, tecnico.getRadioCoberturaKm());
        tecnico.cambiarRadioCobertura(100);
        assertEquals(100, tecnico.getRadioCoberturaKm());

        for (Integer invalido : new Integer[]{null, 0, -3, 101}) {
            RadioCoberturaInvalidoException ex = assertThrows(RadioCoberturaInvalidoException.class,
                    () -> tecnico.cambiarRadioCobertura(invalido));
            assertEquals("El radio de atención debe estar entre 1 y 100 km", ex.getMessage());
        }
        assertEquals(100, tecnico.getRadioCoberturaKm());
    }
}
