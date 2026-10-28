package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificadorCitaHttpAdapterTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private volatile int codigoRespuesta = 200;
    private RegistroLogPort logs;
    private Cita cita;

    @BeforeEach
    void preparar() throws Exception {
        logs = mock(RegistroLogPort.class);
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            cuerpoRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacionRecibida.set(exchange.getRequestHeaders().getFirst("Authorization"));
            rutaRecibida.set(exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(codigoRespuesta, -1);
            exchange.close();
        });
        servidor.start();
        cita = new Cita();
        cita.setId(42L);
        cita.setFechaHoraInicio(LocalDateTime.of(2026, 9, 1, 10, 0));
        cita.setFechaHoraFin(LocalDateTime.of(2026, 9, 1, 11, 0));
    }

    @AfterEach
    void limpiar() {
        servidor.stop(0);
        RequestContextHolder.resetRequestAttributes();
    }

    private NotificadorCitaHttpAdapter adaptador() {
        return new NotificadorCitaHttpAdapter("http://127.0.0.1:" + servidor.getAddress().getPort(), logs);
    }

    @Test
    void sinUrlConfiguradaNoHaceNingunaLlamada() {
        new NotificadorCitaHttpAdapter("", logs).notificarCitaConfirmada(cita, "ana@mail.com");
        new NotificadorCitaHttpAdapter(null, logs).notificarCitaConfirmada(cita, "ana@mail.com");

        assertNull(cuerpoRecibido.get());
        verifyNoInteractions(logs);
    }

    @Test
    void envia_postJsonConDatosDeLaCitaYReenviaElAuthorizationDeLaPeticionActual() {
        MockHttpServletRequest actual = new MockHttpServletRequest();
        actual.addHeader("Authorization", "Bearer abc.def");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(actual));

        adaptador().notificarCitaConfirmada(cita, "ana@mail.com");

        assertEquals("/api/notificaciones/cita-confirmada", rutaRecibida.get());
        assertEquals("Bearer abc.def", autorizacionRecibida.get());
        assertEquals("{\"citaId\":42,\"correoCliente\":\"ana@mail.com\","
                + "\"inicio\":\"2026-09-01T10:00\",\"fin\":\"2026-09-01T11:00\"}", cuerpoRecibido.get());
        verify(logs, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void sinPeticionActualEnviaAuthorizationVacioYEscapaElCorreo() {
        adaptador().notificarCitaConfirmada(cita, "a\"b\\c@mail.com");

        assertEquals("", autorizacionRecibida.get());
        assertTrue(cuerpoRecibido.get().contains("\"correoCliente\":\"a\\\"b\\\\c@mail.com\""));
    }

    @Test
    void notificarCambioDeCitaEnviaEstadoYMensajeAlEndpointDeActualizaciones() {
        cita.setEstado(com.appointech.appointech_backend.domain.models.EstadoCita.CANCELADA);

        adaptador().notificarCambioDeCita(cita, "ana@mail.com", "Tu cita #42 fue cancelada.\nMotivo: \"x\"");

        assertEquals("/api/notificaciones/cita-actualizada", rutaRecibida.get());
        assertEquals("{\"citaId\":42,\"correoDestinatario\":\"ana@mail.com\",\"estado\":\"CANCELADA\","
                + "\"inicio\":\"2026-09-01T10:00\","
                + "\"mensaje\":\"Tu cita #42 fue cancelada.\\nMotivo: \\\"x\\\"\"}", cuerpoRecibido.get());
        verify(logs, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void notificarCambioDeCitaSinUrlConfiguradaNoLlama() {
        new NotificadorCitaHttpAdapter("", logs).notificarCambioDeCita(cita, "ana@mail.com", "hola");

        assertNull(cuerpoRecibido.get());
        verifyNoInteractions(logs);
    }

    @Test
    void notificarCambioDeCitaConServicioCaidoNoPropagaElErrorYLoRegistra() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        NotificadorCitaHttpAdapter adaptador = new NotificadorCitaHttpAdapter("http://127.0.0.1:" + puerto, logs);

        assertDoesNotThrow(() -> adaptador.notificarCambioDeCita(cita, "ana@mail.com", "hola"));

        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), eq("NotificadorCitaHttpAdapter"),
                eq("ana@mail.com"), any());
    }

    @Test
    void correoNuloSeEnviaComoCadenaVacia() {
        adaptador().notificarCitaConfirmada(cita, null);

        assertTrue(cuerpoRecibido.get().contains("\"correoCliente\":\"\""));
    }

    @Test
    void peticionSinCabeceraAuthorizationEnviaVacio() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        adaptador().notificarCitaConfirmada(cita, "ana@mail.com");

        assertEquals("", autorizacionRecibida.get());
    }

    @Test
    void respuestaDeErrorSeRegistraComoAdvertenciaDeIntegracion() {
        codigoRespuesta = 500;

        adaptador().notificarCitaConfirmada(cita, "ana@mail.com");

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), eq("NotificadorCitaHttpAdapter"),
                eq("ana@mail.com"), mensaje.capture());
        assertTrue(mensaje.getValue().contains("500"));
    }

    @Test
    void servicioCaidoNoPropagaLaExcepcionYSeRegistra() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        NotificadorCitaHttpAdapter adaptador = new NotificadorCitaHttpAdapter("http://127.0.0.1:" + puerto, logs);

        assertDoesNotThrow(() -> adaptador.notificarCitaConfirmada(cita, "ana@mail.com"));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), eq("NotificadorCitaHttpAdapter"),
                eq("ana@mail.com"), mensaje.capture());
        assertTrue(mensaje.getValue().startsWith("No se pudo contactar"));
    }

    @Test
    void hiloInterrumpidoConservaLaBanderaYNoRegistraError() {
        Thread.currentThread().interrupt();
        try {
            adaptador().notificarCitaConfirmada(cita, "ana@mail.com");
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
