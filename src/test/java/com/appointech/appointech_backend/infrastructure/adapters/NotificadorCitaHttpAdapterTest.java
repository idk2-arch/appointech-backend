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
import static org.mockito.Mockito.when;

class NotificadorCitaHttpAdapterTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private volatile int codigoRespuesta = 200;
    private RegistroLogPort logs;
    private DatosCorreoCitaResolver resolver;
    private Cita cita;

    @BeforeEach
    void preparar() throws Exception {
        logs = mock(RegistroLogPort.class);
        resolver = mock(DatosCorreoCitaResolver.class);
        when(resolver.resolver(any(Cita.class)))
                .thenReturn(new DatosCorreoCitaResolver.DatosCorreoCita("Ana Lopez", "Plomería", "Luis Perez"));
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
        cita.setDireccionServicio("Calle 1 # 2-3");
    }

    @AfterEach
    void limpiar() {
        servidor.stop(0);
        RequestContextHolder.resetRequestAttributes();
    }

    private NotificadorCitaHttpAdapter adaptador() {
        return new NotificadorCitaHttpAdapter("http://127.0.0.1:" + servidor.getAddress().getPort(), logs, resolver);
    }

    @Test
    void sinUrlConfiguradaNoHaceNingunaLlamada() {
        new NotificadorCitaHttpAdapter("", logs, resolver).notificarCitaConfirmada(cita, "ana@mail.com");
        new NotificadorCitaHttpAdapter(null, logs, resolver).notificarCitaConfirmada(cita, "ana@mail.com");

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
        assertEquals("{\"citaId\":42,\"correoCliente\":\"ana@mail.com\",\"nombreCliente\":\"Ana Lopez\","
                + "\"servicio\":\"Plomería\",\"tecnico\":\"Luis Perez\",\"fecha\":\"2026-09-01\",\"hora\":\"10:00\","
                + "\"direccion\":\"Calle 1 # 2-3\",\"inicio\":\"2026-09-01T10:00\",\"fin\":\"2026-09-01T11:00\"}",
                cuerpoRecibido.get());
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
        new NotificadorCitaHttpAdapter("", logs, resolver).notificarCambioDeCita(cita, "ana@mail.com", "hola");

        assertNull(cuerpoRecibido.get());
        verifyNoInteractions(logs);
    }

    @Test
    void notificarCambioDeCitaConServicioCaidoNoPropagaElErrorYLoRegistra() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        NotificadorCitaHttpAdapter adaptador = new NotificadorCitaHttpAdapter("http://127.0.0.1:" + puerto, logs, resolver);

        assertDoesNotThrow(() -> adaptador.notificarCambioDeCita(cita, "ana@mail.com", "hola"));

        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), eq("NotificadorCitaHttpAdapter"),
                eq("ana@mail.com"), any());
    }

    @Test
    void notificarCitaFinalizadaEnviaClienteServicioTecnicoYDiagnostico() {
        cita.setDiagnostico("Sifón roto; \"reemplazado\"");

        adaptador().notificarCitaFinalizada(cita, "ana@mail.com");

        assertEquals("/api/notificaciones/cita-finalizada", rutaRecibida.get());
        assertEquals("{\"citaId\":42,\"correoCliente\":\"ana@mail.com\",\"nombreCliente\":\"Ana Lopez\","
                + "\"servicio\":\"Plomería\",\"tecnico\":\"Luis Perez\","
                + "\"diagnostico\":\"Sifón roto; \\\"reemplazado\\\"\"}", cuerpoRecibido.get());
        verify(logs, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void notificarCitaFinalizadaSinDiagnosticoEnviaCampoVacio() {
        adaptador().notificarCitaFinalizada(cita, "ana@mail.com");

        assertTrue(cuerpoRecibido.get().contains("\"diagnostico\":\"\""));
    }

    @Test
    void notificarCitaFinalizadaSinUrlConfiguradaNoLlamaNiConsultaDatos() {
        new NotificadorCitaHttpAdapter("", logs, resolver).notificarCitaFinalizada(cita, "ana@mail.com");

        assertNull(cuerpoRecibido.get());
        verifyNoInteractions(logs);
        verify(resolver, never()).resolver(any());
    }

    @Test
    void notificarCitaFinalizadaConRespuestaDeErrorSeRegistraComoAdvertencia() {
        codigoRespuesta = 502;

        adaptador().notificarCitaFinalizada(cita, "ana@mail.com");

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(logs).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION), eq("NotificadorCitaHttpAdapter"),
                eq("ana@mail.com"), mensaje.capture());
        assertTrue(mensaje.getValue().contains("502"));
    }

    @Test
    void siNoSePuedenResolverLosDatosDelCorreoNoSePropagaElErrorYSeRegistra() {
        when(resolver.resolver(any(Cita.class))).thenThrow(new IllegalStateException("sin datos"));

        assertDoesNotThrow(() -> adaptador().notificarCitaFinalizada(cita, "ana@mail.com"));
        assertDoesNotThrow(() -> adaptador().notificarCitaConfirmada(cita, "ana@mail.com"));

        assertNull(cuerpoRecibido.get(), "no se envía nada si el cuerpo no se pudo armar");
        verify(logs, org.mockito.Mockito.times(2)).registrar(eq(NivelLog.WARN), eq(TipoLog.INTEGRACION),
                eq("NotificadorCitaHttpAdapter"), eq("ana@mail.com"), any());
    }

    @Test
    void notificarCitaConfirmadaSinHorarioEnviaFechaYHoraVacias() {
        cita.setFechaHoraInicio(null);

        adaptador().notificarCitaConfirmada(cita, "ana@mail.com");

        assertTrue(cuerpoRecibido.get().contains("\"fecha\":\"\",\"hora\":\"\""));
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
        NotificadorCitaHttpAdapter adaptador = new NotificadorCitaHttpAdapter("http://127.0.0.1:" + puerto, logs, resolver);

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
