package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.TecnicoDisponible;
import com.appointech.appointech_backend.infrastructure.dto.AsignacionManualResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoParaAsignarResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsignacionManualMappersTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);
    private static final LocalDateTime ASIGNADA = LocalDateTime.of(2030, 5, 2, 15, 0);

    private static Cita cita() {
        Cita c = new Cita();
        c.setId(15L);
        c.setEspecialidadId(9L);
        c.setDescripcionProblema("No enfría");
        c.setDireccionServicio("Calle 1");
        c.setFechaHoraInicio(INICIO);
        c.setFechaHoraFin(INICIO.plusHours(2));
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setTecnicoId(3L);
        return c;
    }

    private static Cita citaAsignada(Long adminId, LocalDateTime cuando) {
        Cita c = cita();
        c.setAsignacionManual(true);
        c.setAsignadaPorId(adminId);
        c.setAsignadaEn(cuando);
        return c;
    }

    // ---------- CitaResponse ----------

    @Test
    void citaNormalNoTraeTrazabilidad() {
        CitaResponse r = CitaResponseMapper.aRespuesta(new DetalleCita(cita(), "Ana", "Ruiz", "311", "Aires"));

        assertFalse(r.asignadaManualmente());
        assertNull(r.asignacionManual());
    }

    @Test
    void citaAsignadaManualmenteTraeQuienYCuando() {
        CitaResponse r = CitaResponseMapper.aRespuestaAdmin(
                new DetalleCita(citaAsignada(1L, ASIGNADA), "Ana", "Ruiz", "311", "Aires", null, "Root Admin"));

        assertTrue(r.asignadaManualmente());
        assertEquals(new AsignacionManualResponse(1L, "Root Admin", ASIGNADA), r.asignacionManual());
    }

    @Test
    void filaAntiguaDelBackfillEsAsignadaManualmenteConElObjetoDeDatosNulos() {
        CitaResponse r = CitaResponseMapper.aRespuestaAdmin(
                new DetalleCita(citaAsignada(null, null), "Ana", "Ruiz", "311", "Aires"));

        assertTrue(r.asignadaManualmente());
        assertEquals(new AsignacionManualResponse(null, null, null), r.asignacionManual());
    }

    // ---------- Detalle de cita del admin ----------

    @Test
    void detalleDeCitaNormalNoTraeTrazabilidad() {
        CitaDetalleResponse r = CitaDetalleMapper.aRespuesta(
                new DetalleCitaCompleto(new DetalleCita(cita(), "Ana", "Ruiz", "311", "Aires"), List.of()));

        assertFalse(r.asignadaManualmente());
        assertNull(r.asignacionManual());
    }

    @Test
    void detalleDeCitaAsignadaManualmenteTraeQuienYCuando() {
        CitaDetalleResponse r = CitaDetalleMapper.aRespuestaAdmin(new DetalleCitaCompleto(
                new DetalleCita(citaAsignada(1L, ASIGNADA), "Ana", "Ruiz", "311", "Aires", null, "Root Admin"), List.of()));

        assertTrue(r.asignadaManualmente());
        assertEquals(new AsignacionManualResponse(1L, "Root Admin", ASIGNADA), r.asignacionManual());
    }

    @Test
    void detalleDeFilaAntiguaTraeElObjetoConDatosNulos() {
        CitaDetalleResponse r = CitaDetalleMapper.aRespuestaAdmin(new DetalleCitaCompleto(
                new DetalleCita(citaAsignada(null, null), null, null, null, "Aires"), List.of()));

        assertTrue(r.asignadaManualmente());
        assertEquals(new AsignacionManualResponse(null, null, null), r.asignacionManual());
    }

    @Test
    void elAdministradorVeUnaCitaNormalSinTrazabilidad() {
        DetalleCita normal = new DetalleCita(cita(), "Ana", "Ruiz", "311", "Aires");

        assertFalse(CitaResponseMapper.aRespuestaAdmin(normal).asignadaManualmente());
        assertNull(CitaResponseMapper.aRespuestaAdmin(normal).asignacionManual());
        assertNull(CitaDetalleMapper.aRespuestaAdmin(new DetalleCitaCompleto(normal, List.of())).asignacionManual());
    }

    // ---------- lo que ve el cliente (y el técnico): nunca quién asignó ----------

    @Test
    void elClienteSoloSabeQueSeAsignoManualmenteSinNombreNiIdDelAdministrador() {
        DetalleCita detalle = new DetalleCita(citaAsignada(1L, ASIGNADA), "Ana", "Ruiz", "311", "Aires", null, "Root Admin");

        CitaResponse respuesta = CitaResponseMapper.aRespuesta(detalle);
        CitaDetalleResponse detalleCliente = CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(detalle, List.of()));

        assertTrue(respuesta.asignadaManualmente());
        assertNull(respuesta.asignacionManual());
        assertTrue(detalleCliente.asignadaManualmente());
        assertNull(detalleCliente.asignacionManual());
    }

    // ---------- técnicos para asignar ----------

    @Test
    void tecnicoParaAsignarRedondeaLaDistanciaYArmaLaUrlDeLaFoto() {
        TecnicoParaAsignarResponse r = TecnicoParaAsignarMapper.aRespuesta(
                new TecnicoDisponible(3L, "Ana", "Ruiz", "311", "abc", 3.14159));

        assertEquals(3L, r.tecnicoId());
        assertEquals("Ana", r.nombre());
        assertEquals("Ruiz", r.apellido());
        assertEquals("311", r.telefono());
        assertEquals("/api/fotos/abc", r.fotoUrl());
        assertEquals(3.14, r.distanciaKm());
    }

    @Test
    void tecnicoSinUbicacionNiFotoDejaEsosCamposEnNulo() {
        TecnicoParaAsignarResponse r = TecnicoParaAsignarMapper.aRespuesta(
                new TecnicoDisponible(4L, "Luis", "Mora", null, null, null));

        assertNull(r.distanciaKm());
        assertNull(r.fotoUrl());
        assertNull(r.telefono());
    }

    @Test
    void fechaHoraIsoValidaSeConvierteIgnorandoEspacios() {
        assertEquals(INICIO, TecnicoParaAsignarMapper.aFechaHora("inicio", " 2030-05-06T09:00:00 "));
    }

    @Test
    void fechaHoraFaltanteEnBlancoOMalEscritaEs400ConMensajeLegible() {
        var faltante = assertThrows(FranjaInvalidaException.class, () -> TecnicoParaAsignarMapper.aFechaHora("inicio", null));
        var enBlanco = assertThrows(FranjaInvalidaException.class, () -> TecnicoParaAsignarMapper.aFechaHora("fin", "  "));
        var malEscrita = assertThrows(FranjaInvalidaException.class, () -> TecnicoParaAsignarMapper.aFechaHora("fin", "mañana"));

        assertEquals("inicio es obligatorio (formato 2030-05-06T09:00:00)", faltante.getMessage());
        assertEquals("fin es obligatorio (formato 2030-05-06T09:00:00)", enBlanco.getMessage());
        assertEquals("fin debe tener el formato 2030-05-06T09:00:00", malEscrita.getMessage());
    }
}
