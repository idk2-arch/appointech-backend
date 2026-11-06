package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.CalificacionInvalidaException;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalificacionDominioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 5, 2, 15, 0);

    // ---------- Calificacion ----------

    @Test
    void aceptaLosLimitesDeLaPuntuacion() {
        assertEquals(1, Calificacion.nueva(10L, 1, null, AHORA).puntuacion());
        assertEquals(5, Calificacion.nueva(10L, 5, null, AHORA).puntuacion());
    }

    @Test
    void rechazaPuntuacionNulaCeroOMayorACinco() {
        assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, null, null, AHORA));
        assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, 0, null, AHORA));
        assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, 6, null, AHORA));
        assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, -1, null, AHORA));
    }

    @Test
    void elMensajeDeLaPuntuacionInvalidaEsLegible() {
        var e = assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, 9, null, AHORA));

        assertEquals("La puntuación debe ser un número entero entre 1 y 5", e.getMessage());
    }

    @Test
    void comentarioDeExactamente500CaracteresEsValidoYDe501No() {
        assertEquals(500, Calificacion.nueva(10L, 4, "a".repeat(500), AHORA).comentario().length());

        var e = assertThrows(CalificacionInvalidaException.class, () -> Calificacion.nueva(10L, 4, "a".repeat(501), AHORA));
        assertEquals("El comentario no puede superar los 500 caracteres", e.getMessage());
    }

    @Test
    void comentarioVacioOEnBlancoSeGuardaComoNulo() {
        assertNull(Calificacion.nueva(10L, 4, null, AHORA).comentario());
        assertNull(Calificacion.nueva(10L, 4, "", AHORA).comentario());
        assertNull(Calificacion.nueva(10L, 4, "   \n\t ", AHORA).comentario());
    }

    @Test
    void elComentarioSeGuardaSinEspaciosSobrantes() {
        assertEquals("Muy bien", Calificacion.nueva(10L, 4, "  Muy bien \n", AHORA).comentario());
    }

    @Test
    void laCalificacionNuevaNoTieneIdYConservaCitaYFecha() {
        Calificacion c = Calificacion.nueva(10L, 3, "ok", AHORA);

        assertNull(c.id());
        assertEquals(10L, c.citaId());
        assertEquals(AHORA, c.creadaEn());
    }

    // ---------- ConfiguracionCalificacion ----------

    @Test
    void elLimiteEsLaFinalizacionMasElPlazo() {
        assertEquals(AHORA.plusDays(30), new ConfiguracionCalificacion(30).limite(AHORA));
    }

    @Test
    void elPlazoDebeSerDeAlMenosUnDia() {
        assertEquals(1, new ConfiguracionCalificacion(1).plazoDias());
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionCalificacion(0));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionCalificacion(-5));
    }

    // ---------- PaginacionOpcional ----------

    @Test
    void sinPaginaNiTamanioNoSePagina() {
        PaginacionOpcional p = PaginacionOpcional.de(null, null);

        assertFalse(p.paginado());
        assertNull(p.pagina());
        assertNull(p.tamanio());
    }

    @Test
    void conUnoSoloElOtroTomaSuValorPorDefecto() {
        assertEquals(new PaginacionOpcional(2, PaginacionOpcional.TAMANIO_POR_DEFECTO), PaginacionOpcional.de(2, null));
        assertEquals(new PaginacionOpcional(0, 5), PaginacionOpcional.de(null, 5));
        assertTrue(PaginacionOpcional.de(0, 5).paginado());
    }

    @Test
    void aceptaLosLimitesDelTamanio() {
        assertEquals(1, PaginacionOpcional.de(0, 1).tamanio());
        assertEquals(100, PaginacionOpcional.de(0, 100).tamanio());
    }

    @Test
    void rechazaPaginaNegativaYTamanioFueraDeRango() {
        assertThrows(FiltroCitasInvalidoException.class, () -> PaginacionOpcional.de(-1, 10));
        assertThrows(FiltroCitasInvalidoException.class, () -> PaginacionOpcional.de(0, 0));
        assertThrows(FiltroCitasInvalidoException.class, () -> PaginacionOpcional.de(0, 101));
    }
}
