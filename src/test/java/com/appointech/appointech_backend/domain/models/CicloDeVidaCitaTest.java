package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CicloDeVidaCitaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 9, 0);

    private Cita citaEn(EstadoCita estado) {
        Cita c = new Cita();
        c.setEstado(estado);
        return c;
    }

    // ---- EstadoCita ----

    @Test
    void soloExisteUnCaminoDeAvance() {
        assertEquals(Optional.of(EstadoCita.EN_CAMINO), EstadoCita.CONFIRMADA.siguiente());
        assertEquals(Optional.of(EstadoCita.EN_PROCESO), EstadoCita.EN_CAMINO.siguiente());
        assertEquals(Optional.of(EstadoCita.FINALIZADA), EstadoCita.EN_PROCESO.siguiente());
        assertTrue(EstadoCita.PENDIENTE.siguiente().isEmpty());
        assertTrue(EstadoCita.FINALIZADA.siguiente().isEmpty());
        assertTrue(EstadoCita.CANCELADA.siguiente().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(EstadoCita.class)
    void soloLasTresTransicionesPermitidasSeAceptan(EstadoCita origen) {
        for (EstadoCita destino : EstadoCita.values()) {
            boolean esperado = (origen == EstadoCita.CONFIRMADA && destino == EstadoCita.EN_CAMINO)
                    || (origen == EstadoCita.EN_CAMINO && destino == EstadoCita.EN_PROCESO)
                    || (origen == EstadoCita.EN_PROCESO && destino == EstadoCita.FINALIZADA);
            assertEquals(esperado, origen.puedeAvanzarA(destino), origen + " -> " + destino);
        }
    }

    @Test
    void soloFinalizadaYCanceladaSonFinales() {
        for (EstadoCita e : EstadoCita.values()) {
            assertEquals(e == EstadoCita.FINALIZADA || e == EstadoCita.CANCELADA, e.esFinal(), e.name());
        }
    }

    @Test
    void elClienteSoloModificaPendienteOConfirmada() {
        for (EstadoCita e : EstadoCita.values()) {
            assertEquals(e == EstadoCita.PENDIENTE || e == EstadoCita.CONFIRMADA,
                    e.admiteCancelacionOReprogramacionDelCliente(), e.name());
        }
    }

    @Test
    void lasFotosDelElectrodomesticoSoloSeAdmitenEnPendienteOConfirmada() {
        for (EstadoCita e : EstadoCita.values()) {
            assertEquals(e == EstadoCita.PENDIENTE || e == EstadoCita.CONFIRMADA,
                    e.admiteFotosDelElectrodomestico(), e.name());
        }
    }

    // ---- Cita.avanzarA ----

    @Test
    void avanzarAplicaLaTransicionPermitida() {
        Cita cita = citaEn(EstadoCita.CONFIRMADA);

        cita.avanzarA(EstadoCita.EN_CAMINO);

        assertEquals(EstadoCita.EN_CAMINO, cita.getEstado());
    }

    @Test
    void avanzarSaltandoUnPasoFallaYDejaElEstadoIntacto() {
        Cita cita = citaEn(EstadoCita.CONFIRMADA);

        EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class,
                () -> cita.avanzarA(EstadoCita.EN_PROCESO));

        assertTrue(ex.getMessage().contains("CONFIRMADA") && ex.getMessage().contains("EN_PROCESO"));
        assertTrue(ex.getMessage().contains("EN_CAMINO"), "debe indicar el único cambio permitido");
        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
    }

    @Test
    void avanzarHaciaAtrasFalla() {
        Cita cita = citaEn(EstadoCita.EN_PROCESO);

        assertThrows(EstadoCitaInvalidoException.class, () -> cita.avanzarA(EstadoCita.EN_CAMINO));
    }

    @Test
    void avanzarACanceladaNoEsUnaTransicionDelTecnico() {
        assertThrows(EstadoCitaInvalidoException.class, () -> citaEn(EstadoCita.EN_CAMINO).avanzarA(EstadoCita.CANCELADA));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"FINALIZADA", "CANCELADA"})
    void desdeUnEstadoFinalNoSePuedeCambiarNada(EstadoCita estadoFinal) {
        Cita cita = citaEn(estadoFinal);

        for (EstadoCita destino : EstadoCita.values()) {
            EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class, () -> cita.avanzarA(destino));
            assertTrue(ex.getMessage().contains("no admite cambios"));
        }
        assertEquals(estadoFinal, cita.getEstado());
    }

    @Test
    void unaCitaSinEstadoNoPuedeAvanzar() {
        assertThrows(EstadoCitaInvalidoException.class, () -> new Cita().avanzarA(EstadoCita.EN_CAMINO));
    }

    @Test
    void avanzarDesdePendienteIndicaQueNoHayCambioPermitido() {
        EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class,
                () -> citaEn(EstadoCita.PENDIENTE).avanzarA(EstadoCita.EN_CAMINO));

        assertTrue(ex.getMessage().contains("ninguno"));
    }

    // ---- cancelar ----

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA"})
    void elClientePuedeCancelarPendienteYConfirmada(EstadoCita estado) {
        Cita cita = citaEn(estado);

        cita.cancelarPorCliente();

        assertEquals(EstadoCita.CANCELADA, cita.getEstado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"EN_CAMINO", "EN_PROCESO", "FINALIZADA", "CANCELADA"})
    void elClienteNoPuedeCancelarUnaCitaYaEnEjecucionOCerrada(EstadoCita estado) {
        Cita cita = citaEn(estado);

        assertThrows(EstadoCitaInvalidoException.class, cita::cancelarPorCliente);
        assertEquals(estado, cita.getEstado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"CONFIRMADA", "EN_CAMINO", "EN_PROCESO"})
    void elTecnicoPuedeCancelarMientrasLaCitaNoEsteCerrada(EstadoCita estado) {
        Cita cita = citaEn(estado);

        cita.cancelarPorTecnico();

        assertEquals(EstadoCita.CANCELADA, cita.getEstado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"FINALIZADA", "CANCELADA"})
    void elTecnicoNoPuedeCancelarUnaCitaCerrada(EstadoCita estado) {
        Cita cita = citaEn(estado);

        assertThrows(EstadoCitaInvalidoException.class, cita::cancelarPorTecnico);
        assertEquals(estado, cita.getEstado());
    }

    @Test
    void cancelarSinEstadoFalla() {
        assertThrows(EstadoCitaInvalidoException.class, () -> new Cita().cancelarPorTecnico());
        assertThrows(EstadoCitaInvalidoException.class, () -> new Cita().cancelarPorCliente());
    }

    // ---- reprogramar ----

    @Test
    void reprogramarCambiaLasHorasYConservaElEstado() {
        Cita cita = citaEn(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(AHORA);
        cita.setFechaHoraFin(AHORA.plusHours(2));

        cita.reprogramar(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2));

        assertEquals(AHORA.plusDays(1), cita.getFechaHoraInicio());
        assertEquals(AHORA.plusDays(1).plusHours(2), cita.getFechaHoraFin());
        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
    }

    @Test
    void reprogramarEnUnEstadoNoPermitidoFallaSinTocarLasHoras() {
        Cita cita = citaEn(EstadoCita.EN_CAMINO);
        cita.setFechaHoraInicio(AHORA);

        EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class,
                () -> cita.reprogramar(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2)));

        assertTrue(ex.getMessage().contains("reprogramar"));
        assertEquals(AHORA, cita.getFechaHoraInicio());
    }

    // ---- diagnóstico ----

    @Test
    void registrarDiagnosticoGuardaTextoRecomendacionesYFecha() {
        Cita cita = citaEn(EstadoCita.EN_PROCESO);

        cita.registrarDiagnostico("  Fuga en el sifón  ", "  Cambiar el sifón ", AHORA);

        assertEquals("Fuga en el sifón", cita.getDiagnostico());
        assertEquals("Cambiar el sifón", cita.getRecomendaciones());
        assertEquals(AHORA, cita.getFechaDiagnostico());
    }

    @Test
    void lasRecomendacionesSonOpcionales() {
        Cita cita = citaEn(EstadoCita.EN_PROCESO);

        cita.registrarDiagnostico("Fuga", "   ", AHORA);

        assertNull(cita.getRecomendaciones());
        cita.registrarDiagnostico("Fuga", null, AHORA);
        assertNull(cita.getRecomendaciones());
    }

    @Test
    void unSegundoDiagnosticoLoReemplazaMientrasSigaEnProceso() {
        Cita cita = citaEn(EstadoCita.EN_PROCESO);
        cita.registrarDiagnostico("Primero", "r1", AHORA);

        cita.registrarDiagnostico("Corregido", null, AHORA.plusMinutes(10));

        assertEquals("Corregido", cita.getDiagnostico());
        assertNull(cita.getRecomendaciones());
        assertEquals(AHORA.plusMinutes(10), cita.getFechaDiagnostico());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA", "EN_CAMINO", "FINALIZADA", "CANCELADA"})
    void elDiagnosticoSoloSeRegistraEnProceso(EstadoCita estado) {
        Cita cita = citaEn(estado);

        EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class,
                () -> cita.registrarDiagnostico("Algo", null, AHORA));

        assertTrue(ex.getMessage().contains("EN_PROCESO"));
        assertNull(cita.getDiagnostico());
    }

    @Test
    void elDiagnosticoEnBlancoSeRechaza() {
        Cita cita = citaEn(EstadoCita.EN_PROCESO);

        assertThrows(DatosCitaInvalidosException.class, () -> cita.registrarDiagnostico("  ", "r", AHORA));
        assertThrows(DatosCitaInvalidosException.class, () -> cita.registrarDiagnostico(null, "r", AHORA));
        assertNull(cita.getDiagnostico());
    }
}
