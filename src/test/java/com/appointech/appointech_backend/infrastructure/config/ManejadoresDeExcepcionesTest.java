package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.exception.CitaSinUbicacionException;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.PerfilTecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudManualNoPermitidaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.TelefonoContactoRequeridoException;
import com.appointech.appointech_backend.domain.exception.UbicacionBaseNoConfiguradaException;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ManejadoresDeExcepcionesTest {

    private final GlobalExceptionHandler global = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");
    private RegistroLogPort logs;
    private AgendamientoExceptionHandler agendamiento;

    @BeforeEach
    void preparar() {
        logs = mock(RegistroLogPort.class);
        agendamiento = new AgendamientoExceptionHandler(logs);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void verificar(ProblemDetail p, HttpStatus esperado) {
        assertEquals(esperado.value(), p.getStatus());
        assertEquals("/api/prueba", p.getInstance().getPath());
        assertNotNull(p.getDetail());
    }

    @Test
    void conflictosDeNegocioResponden409() {
        verificar(global.manejarCorreoYaRegistrado(new CorreoYaRegistradoException("a@x.com"), request), HttpStatus.CONFLICT);
        verificar(global.manejarDocumentoYaRegistrado(new DocumentoYaRegistradoException("1"), request), HttpStatus.CONFLICT);
        verificar(global.manejarSolicitudManualNoPermitida(new SolicitudManualNoPermitidaException("no"), request), HttpStatus.CONFLICT);
    }

    @Test
    void errorDeEstadoDeCitaResponde409YDatosDeCitaInvalidosResponde400() {
        ResponseEntity<Map<String, Object>> conflicto = agendamiento.manejarEstadoCitaInvalido(
                new com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException("no se puede"));
        ResponseEntity<Map<String, Object>> invalido = agendamiento.manejarDatosCitaInvalidos(
                new com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException("motivo"));

        assertEquals(HttpStatus.CONFLICT, conflicto.getStatusCode());
        assertEquals("no se puede", conflicto.getBody().get("error"));
        assertEquals(HttpStatus.BAD_REQUEST, invalido.getStatusCode());
        assertEquals(400, invalido.getBody().get("status"));
    }

    @Test
    void datosInvalidosResponden400() {
        verificar(global.manejarDatosInvalidos(new RadioCoberturaInvalidoException("radio"), request), HttpStatus.BAD_REQUEST);
        verificar(global.manejarDatosInvalidos(new TelefonoContactoRequeridoException(), request), HttpStatus.BAD_REQUEST);
        verificar(global.manejarEspecialidadNoEncontrada(new EspecialidadNoEncontradaException(List.of(1L)), request),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void noEncontradosResponden404() {
        verificar(global.manejarTecnicoNoEncontrado(new TecnicoNoEncontradoException(1L), request), HttpStatus.NOT_FOUND);
    }

    @Test
    void accesoDenegadoResponde403ConMensajeFijoSinFiltrarElDetalleInterno() {
        ProblemDetail p = global.manejarAccesoDenegado(new AccessDeniedException("detalle interno"), request);

        verificar(p, HttpStatus.FORBIDDEN);
        assertEquals("No tienes permisos para realizar esta acción", p.getDetail());
    }

    // ---- AgendamientoExceptionHandler ----

    @Test
    void erroresDeAgendamientoMapeanAStatusEsperadoYDevuelvenElMensaje() {
        record Caso(ResponseEntity<Map<String, Object>> r, HttpStatus esperado) {}
        List<Caso> casos = List.of(
                new Caso(agendamiento.manejarClienteNoEncontrado(new ClienteNoEncontradoException("a@x.com")), HttpStatus.NOT_FOUND),
                new Caso(agendamiento.manejarSolicitudNoEncontrada(new SolicitudNoEncontradaException(1L)), HttpStatus.NOT_FOUND),
                new Caso(agendamiento.manejarFranjaNoDisponible(new FranjaNoDisponibleException("ocupada")), HttpStatus.CONFLICT),
                new Caso(agendamiento.manejarFranjaInvalida(new FranjaInvalidaException("mala")), HttpStatus.BAD_REQUEST),
                new Caso(agendamiento.manejarPerfilTecnicoNoEncontrado(new PerfilTecnicoNoEncontradoException("t@x.com")), HttpStatus.NOT_FOUND),
                new Caso(agendamiento.manejarUbicacionBaseNoConfigurada(new UbicacionBaseNoConfiguradaException()), HttpStatus.CONFLICT),
                new Caso(agendamiento.manejarCitaSinUbicacion(new CitaSinUbicacionException(3L)), HttpStatus.CONFLICT));

        for (Caso c : casos) {
            assertEquals(c.esperado(), c.r().getStatusCode());
            assertEquals(c.esperado().value(), c.r().getBody().get("status"));
            assertNotNull(c.r().getBody().get("error"));
            assertNotNull(c.r().getBody().get("timestamp"));
        }
    }

    @Test
    void conflictoSeRegistraComoWarnYNoEncontradoComoInfoConElUsuarioAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ana@mail.com", null));

        agendamiento.manejarFranjaNoDisponible(new FranjaNoDisponibleException("ocupada"));
        agendamiento.manejarSolicitudNoEncontrada(new SolicitudNoEncontradaException(8L));

        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.NEGOCIO), eq("AgendamientoExceptionHandler"),
                eq("ana@mail.com"), eq("409 - ocupada"));
        verify(logs).registrar(eq(NivelLog.INFO), eq(TipoLog.NEGOCIO), eq("AgendamientoExceptionHandler"),
                eq("ana@mail.com"), any());
    }

    @Test
    void sinAutenticacionElLogSeRegistraConUsuarioNulo() {
        agendamiento.manejarFranjaInvalida(new FranjaInvalidaException("mala"));

        verify(logs).registrar(eq(NivelLog.INFO), eq(TipoLog.NEGOCIO), eq("AgendamientoExceptionHandler"),
                eq(null), eq("400 - mala"));
    }
}
