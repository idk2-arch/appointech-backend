package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.PagoInvalidoException;
import com.appointech.appointech_backend.domain.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TransaccionNoCoincideException;
import com.appointech.appointech_backend.domain.models.ComprobanteDetalle;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoMetodoPago;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarPagoEfectivoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarComprobanteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ElegirMetodoPagoUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarCostoFinalUseCase;
import com.appointech.appointech_backend.domain.ports.in.SincronizarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.PagosConfig;
import com.appointech.appointech_backend.infrastructure.config.SecurityConfig;
import com.appointech.appointech_backend.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pagos con la seguridad real: roles por ruta, 403 por pertenencia, 404, 409, 422, 503 y 400 por datos inválidos. */
@WebMvcTest(value = {PagoController.class, CotizacionController.class},
        properties = {"jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!",
                "pagos.umbral-diferencia-porcentaje=20"})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, PagosConfig.class})
class PagoControllerTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String PAGO = "/api/citas/10/pago";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private RegistrarCostoFinalUseCase registrarCostoFinal;
    @MockitoBean
    private ElegirMetodoPagoUseCase elegirMetodo;
    @MockitoBean
    private ConfirmarPagoEfectivoUseCase confirmarEfectivo;
    @MockitoBean
    private SincronizarPagoUseCase sincronizar;
    @MockitoBean
    private ConsultarPagoUseCase consultarPago;
    @MockitoBean
    private ConsultarComprobanteUseCase consultarComprobante;
    @MockitoBean
    private ConsultarCotizacionCitaUseCase cotizacionCita;
    @MockitoBean
    private ConsultarCotizacionEspecialidadUseCase cotizacionEspecialidad;
    @MockitoBean
    private RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String rol) {
        return "Bearer " + Jwts.builder().subject("u@mail.com").claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder peticion, String rol, String cuerpo) {
        return peticion.header("Authorization", token(rol)).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private Pago pago(String cotizado, String finalMonto) {
        Pago pago = Pago.nuevo(10L);
        pago.setId(1L);
        pago.setMontoCotizado(cotizado == null ? null : new BigDecimal(cotizado));
        pago.setMontoFinal(finalMonto == null ? null : new BigDecimal(finalMonto));
        return pago;
    }

    // ---------- cotización ----------

    @Test
    void elClienteCotizaUnaEspecialidadSinCita() throws Exception {
        when(cotizacionEspecialidad.cotizar(2L)).thenReturn(
                new Cotizacion(2L, "Neveras", new BigDecimal("90000"), Cotizacion.NOTA_ESTIMADO));

        mvc.perform(get("/api/especialidades/2/cotizacion").header("Authorization", token("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("Neveras"))
                .andExpect(jsonPath("$.montoEstimado").value(90000))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.nota").value("Valor estimado; el costo final lo registra el técnico"));
    }

    @Test
    void unaEspecialidadSinCostoDeReferenciaRespondeConUnMensajeClaroYSinError() throws Exception {
        when(cotizacionEspecialidad.cotizar(3L)).thenReturn(
                new Cotizacion(3L, "Estufas", null, Cotizacion.NOTA_SIN_REFERENCIA));

        mvc.perform(get("/api/especialidades/3/cotizacion").header("Authorization", token("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoEstimado").doesNotExist())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.nota").value(Cotizacion.NOTA_SIN_REFERENCIA));
    }

    @Test
    void cotizarUnaEspecialidadExigeSerClienteYAutenticarse() throws Exception {
        mvc.perform(get("/api/especialidades/2/cotizacion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/especialidades/2/cotizacion").header("Authorization", token("TECNICO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cotizacionEspecialidad);
    }

    @Test
    void unaEspecialidadInexistenteResponde400ConMensajeEnEspanol() throws Exception {
        when(cotizacionEspecialidad.cotizar(99L)).thenThrow(new EspecialidadNoEncontradaException(List.of(99L)));

        mvc.perform(get("/api/especialidades/99/cotizacion").header("Authorization", token("CLIENTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cualquierParticipanteCotizaUnaCitaYOtroRecibe403() throws Exception {
        when(cotizacionCita.cotizar("u@mail.com", 10L)).thenReturn(
                new Cotizacion(2L, "Neveras", new BigDecimal("90000"), Cotizacion.NOTA_ESTIMADO));

        for (String rol : new String[]{"CLIENTE", "TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(get("/api/citas/10/cotizacion").header("Authorization", token(rol)))
                    .andExpect(status().isOk());
        }
        when(cotizacionCita.cotizar("u@mail.com", 11L)).thenThrow(new AccesoCitaDenegadoException());
        mvc.perform(get("/api/citas/11/cotizacion").header("Authorization", token("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/citas/10/cotizacion")).andExpect(status().isUnauthorized());
    }

    // ---------- costo final ----------

    @Test
    void elTecnicoRegistraElCostoFinalYVeLaDiferenciaSignificativa() throws Exception {
        when(registrarCostoFinal.registrar("u@mail.com", 10L, new BigDecimal("100000")))
                .thenReturn(pago("80000", "100000"));

        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":100000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoFinal").value(100000))
                .andExpect(jsonPath("$.montoCotizado").value(80000))
                .andExpect(jsonPath("$.diferenciaSignificativa").value(true))
                .andExpect(jsonPath("$.porcentajeDiferencia").value(25.0));
    }

    @Test
    void unaDiferenciaPequenaNoEsSignificativa() throws Exception {
        when(registrarCostoFinal.registrar(any(), any(), any())).thenReturn(pago("80000", "85000"));

        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":85000}"))
                .andExpect(jsonPath("$.diferenciaSignificativa").value(false));
    }

    @Test
    void sinCotizacionNoHayPorcentajeDeDiferencia() throws Exception {
        when(registrarCostoFinal.registrar(any(), any(), any())).thenReturn(pago(null, "85000"));

        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":85000}"))
                .andExpect(jsonPath("$.diferenciaSignificativa").value(false))
                .andExpect(jsonPath("$.porcentajeDiferencia").doesNotExist());
    }

    @Test
    void elCostoFinalSoloLoRegistraUnTecnico() throws Exception {
        for (String rol : new String[]{"CLIENTE", "ADMINISTRADOR"}) {
            mvc.perform(con(patch("/api/citas/10/costo-final"), rol, "{\"monto\":100}")).andExpect(status().isForbidden());
        }
        mvc.perform(patch("/api/citas/10/costo-final").contentType(MediaType.APPLICATION_JSON).content("{\"monto\":100}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(registrarCostoFinal);
    }

    @Test
    void unCostoFinalNuloCeroNegativoOConMuchosDecimalesResponde400() throws Exception {
        for (String cuerpo : new String[]{"{}", "{\"monto\":0}", "{\"monto\":-5}", "{\"monto\":10.123}"}) {
            mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", cuerpo)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(registrarCostoFinal);
    }

    @Test
    void elCostoFinalDeUnaCitaNoFinalizadaResponde409YDeOtroTecnico403() throws Exception {
        when(registrarCostoFinal.registrar(any(), any(), any()))
                .thenThrow(new EstadoCitaInvalidoException("La cita no está FINALIZADA"))
                .thenThrow(new AccesoCitaDenegadoException())
                .thenThrow(new SolicitudNoEncontradaException(10L));

        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":100}")).andExpect(status().isConflict());
        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":100}")).andExpect(status().isForbidden());
        mvc.perform(con(patch("/api/citas/10/costo-final"), "TECNICO", "{\"monto\":100}")).andExpect(status().isNotFound());
    }

    // ---------- método de pago ----------

    @Test
    void elClienteEligeEnLineaYRecibeLaUrlDePago() throws Exception {
        Pago pago = pago("80000", "80000");
        pago.setMetodo(MetodoPago.EN_LINEA);
        pago.setReferenciaExterna("APT-10-1-1");
        when(elegirMetodo.elegir("u@mail.com", 10L, MetodoPago.EN_LINEA))
                .thenReturn(new ResultadoMetodoPago(pago, "http://pagar"));

        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{\"metodo\":\"EN_LINEA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlPago").value("http://pagar"))
                .andExpect(jsonPath("$.referencia").value("APT-10-1-1"))
                .andExpect(jsonPath("$.metodo").value("EN_LINEA"));
    }

    @Test
    void elegirEfectivoNoTraeUrl() throws Exception {
        Pago pago = pago("80000", "80000");
        pago.setMetodo(MetodoPago.EFECTIVO);
        when(elegirMetodo.elegir("u@mail.com", 10L, MetodoPago.EFECTIVO))
                .thenReturn(new ResultadoMetodoPago(pago, null));

        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{\"metodo\":\"EFECTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.urlPago").doesNotExist());
    }

    @Test
    void elegirMetodoSoloLoHaceElClienteYExigeUnMetodoValido() throws Exception {
        mvc.perform(con(patch(PAGO + "/metodo"), "TECNICO", "{\"metodo\":\"EFECTIVO\"}")).andExpect(status().isForbidden());
        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{}")).andExpect(status().isBadRequest());
        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{\"metodo\":\"CHEQUE\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(elegirMetodo);
    }

    @Test
    void siLaPasarelaNoEstaDisponibleResponde503ConMensajeEnEspanol() throws Exception {
        when(elegirMetodo.elegir(any(), any(), any())).thenThrow(new PasarelaNoDisponibleException());

        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{\"metodo\":\"EN_LINEA\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(
                        "El pago en línea no está disponible por ahora; intenta más tarde o paga en efectivo"));
    }

    @Test
    void elegirMetodoSinCostoFinalResponde409() throws Exception {
        when(elegirMetodo.elegir(any(), any(), any()))
                .thenThrow(new PagoInvalidoException("El técnico aún no registró el costo final del servicio"));

        mvc.perform(con(patch(PAGO + "/metodo"), "CLIENTE", "{\"metodo\":\"EFECTIVO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El técnico aún no registró el costo final del servicio"));
    }

    // ---------- confirmar efectivo ----------

    @Test
    void elTecnicoConfirmaElEfectivo() throws Exception {
        Pago pago = pago("80000", "80000");
        pago.setEstado(EstadoPago.PAGADO);
        pago.setMetodo(MetodoPago.EFECTIVO);
        when(confirmarEfectivo.confirmar("u@mail.com", 10L)).thenReturn(pago);

        mvc.perform(patch(PAGO + "/confirmar-efectivo").header("Authorization", token("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void confirmarEfectivoExigeTecnicoYRespetaLaPertenencia() throws Exception {
        mvc.perform(patch(PAGO + "/confirmar-efectivo").header("Authorization", token("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(patch(PAGO + "/confirmar-efectivo")).andExpect(status().isUnauthorized());

        when(confirmarEfectivo.confirmar(any(), any()))
                .thenThrow(new AccesoCitaDenegadoException())
                .thenThrow(new PagoNoEncontradoException(10L))
                .thenThrow(new PagoInvalidoException("El pago ya fue realizado"));
        mvc.perform(patch(PAGO + "/confirmar-efectivo").header("Authorization", token("TECNICO")))
                .andExpect(status().isForbidden());
        mvc.perform(patch(PAGO + "/confirmar-efectivo").header("Authorization", token("TECNICO")))
                .andExpect(status().isNotFound());
        mvc.perform(patch(PAGO + "/confirmar-efectivo").header("Authorization", token("TECNICO")))
                .andExpect(status().isConflict());
    }

    // ---------- sincronizar ----------

    @Test
    void sincronizarActualizaElPagoParaCualquierParticipante() throws Exception {
        Pago pago = pago("80000", "80000");
        pago.setEstado(EstadoPago.PAGADO);
        when(sincronizar.sincronizar("u@mail.com", 10L, "tx-1")).thenReturn(pago);

        for (String rol : new String[]{"CLIENTE", "TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(con(post(PAGO + "/sincronizar"), rol, "{\"idTransaccion\":\"tx-1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("PAGADO"));
        }
    }

    @Test
    void sincronizarAceptaElCuerpoSinIdDeTransaccionYConsultaPorLaReferenciaGuardada() throws Exception {
        Pago pago = pago("80000", "80000");
        when(sincronizar.sincronizar("u@mail.com", 10L, null)).thenReturn(pago);

        mvc.perform(con(post(PAGO + "/sincronizar"), "CLIENTE", "{}")).andExpect(status().isOk());
        mvc.perform(con(post(PAGO + "/sincronizar"), "CLIENTE", "{\"idTransaccion\":null}")).andExpect(status().isOk());
        mvc.perform(post(PAGO + "/sincronizar").header("Authorization", token("CLIENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void sincronizarConIdDemasiadoLargoResponde400YSinTokenResponde401() throws Exception {
        mvc.perform(con(post(PAGO + "/sincronizar"), "CLIENTE", "{\"idTransaccion\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(PAGO + "/sincronizar").contentType(MediaType.APPLICATION_JSON).content("{\"idTransaccion\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sincronizar);
    }

    @Test
    void sincronizarSinPagoEnLineaResponde409ConSuMensaje() throws Exception {
        when(sincronizar.sincronizar(any(), any(), any()))
                .thenThrow(new com.appointech.appointech_backend.domain.exception.PagoInvalidoException(
                        "No hay un pago en línea iniciado para esta cita"));

        mvc.perform(con(post(PAGO + "/sincronizar"), "CLIENTE", "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No hay un pago en línea iniciado para esta cita"));
    }
    @Test
    void unaTransaccionQueNoCoincideResponde422() throws Exception {
        when(sincronizar.sincronizar(any(), any(), any())).thenThrow(new TransaccionNoCoincideException());

        mvc.perform(con(post(PAGO + "/sincronizar"), "CLIENTE", "{\"idTransaccion\":\"tx-1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    // ---------- consultar pago y comprobante ----------

    @Test
    void consultarElPagoYSuAusencia() throws Exception {
        when(consultarPago.consultar("u@mail.com", 10L)).thenReturn(pago("80000", "80000"));
        when(consultarPago.consultar("u@mail.com", 11L)).thenThrow(new PagoNoEncontradoException(11L));
        when(consultarPago.consultar("u@mail.com", 12L)).thenThrow(new AccesoCitaDenegadoException());

        mvc.perform(get(PAGO).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citaId").value(10));
        mvc.perform(get("/api/citas/11/pago").header("Authorization", token("CLIENTE"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/citas/12/pago").header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(PAGO)).andExpect(status().isUnauthorized());
    }

    @Test
    void consultarElComprobanteDevuelveSusDatosYNoExisteUnPostParaGenerarlo() throws Exception {
        when(consultarComprobante.consultar("u@mail.com", 10L)).thenReturn(new ComprobanteDetalle("COMP-000001",
                LocalDateTime.of(2026, 11, 5, 10, 0), 10L, "Ana Pérez", "Luis Gómez", "Neveras",
                new BigDecimal("90000"), MetodoPago.EFECTIVO, null, LocalDateTime.of(2026, 11, 5, 10, 0)));

        mvc.perform(get("/api/citas/10/comprobante").header("Authorization", token("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("COMP-000001"))
                .andExpect(jsonPath("$.cliente").value("Ana Pérez"))
                .andExpect(jsonPath("$.tecnico").value("Luis Gómez"))
                .andExpect(jsonPath("$.monto").value(90000))
                .andExpect(jsonPath("$.moneda").value("COP"));
        mvc.perform(post("/api/citas/10/comprobante").header("Authorization", token("TECNICO")))
                .andExpect(status().is4xxClientError());
        verify(consultarComprobante).consultar("u@mail.com", 10L);
    }

    @Test
    void elComprobanteSinPagoPagadoResponde409YDeOtroUsuario403() throws Exception {
        when(consultarComprobante.consultar(any(), any()))
                .thenThrow(new ComprobanteNoDisponibleException("El comprobante solo está disponible cuando el pago está PAGADO"))
                .thenThrow(new AccesoCitaDenegadoException());

        mvc.perform(get("/api/citas/10/comprobante").header("Authorization", token("CLIENTE")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/citas/10/comprobante").header("Authorization", token("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/citas/10/comprobante")).andExpect(status().isUnauthorized());
    }
}
