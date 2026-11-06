package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.domain.ports.in.ConsultarHistorialPagosUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.SecurityConfig;
import com.appointech.appointech_backend.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/admin/pagos con la seguridad real (JWT, roles, CORS) y el manejador de errores real. */
@WebMvcTest(value = AdminPagoController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminPagoControllerHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/admin/pagos";

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private ConsultarHistorialPagosUseCase consultar;
    @MockitoBean
    private com.appointech.appointech_backend.domain.ports.out.RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String rol) {
        return "Bearer " + Jwts.builder().subject("u@mail.com").claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static ListadoConTotal<PagoDelHistorial> dosPagos(long total) {
        return new ListadoConTotal<>(List.of(
                new PagoDelHistorial(10L, 2L, "Ana Ruiz", 3L, "Luis Mora", "Plomería", new BigDecimal("100000.00"),
                        new BigDecimal("95000.00"), MetodoPago.EN_LINEA, EstadoPago.PAGADO,
                        LocalDateTime.of(2030, 5, 10, 9, 30), "COMP-000001"),
                new PagoDelHistorial(11L, 2L, "Ana Ruiz", null, null, "Neveras", new BigDecimal("80000.00"),
                        null, null, EstadoPago.PENDIENTE, null, null)), total);
    }

    private ArgumentCaptor<FiltroHistorialPagos> verificarLlamada() {
        ArgumentCaptor<FiltroHistorialPagos> captor = ArgumentCaptor.forClass(FiltroHistorialPagos.class);
        verify(consultar).consultar(captor.capture());
        return captor;
    }

    @Test
    void devuelveUnArregloConElContratoExactoYElTotalEnElEncabezado() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(57));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "57"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].citaId").value(10))
                .andExpect(jsonPath("$[0].clienteId").value(2))
                .andExpect(jsonPath("$[0].clienteNombre").value("Ana Ruiz"))
                .andExpect(jsonPath("$[0].tecnicoId").value(3))
                .andExpect(jsonPath("$[0].tecnicoNombre").value("Luis Mora"))
                .andExpect(jsonPath("$[0].especialidad").value("Plomería"))
                .andExpect(jsonPath("$[0].montoCotizado").value(100000.00))
                .andExpect(jsonPath("$[0].montoFinal").value(95000.00))
                .andExpect(jsonPath("$[0].metodo").value("EN_LINEA"))
                .andExpect(jsonPath("$[0].estado").value("PAGADO"))
                .andExpect(jsonPath("$[0].pagadoEn").value("2030-05-10T09:30:00"))
                .andExpect(jsonPath("$[0].numeroComprobante").value("COMP-000001"));
    }

    @Test
    void unPagoPendienteTraeLosCamposAusentesEnNuloIncluidoElComprobante() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(2));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(jsonPath("$[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].numeroComprobante").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].metodo").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].montoFinal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].pagadoEn").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].tecnicoId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void noExponeDatosDeLaPasarelaNiOtrosCamposQueLosDelContrato() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(2));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(jsonPath("$[0].referenciaExterna").doesNotExist())
                .andExpect(jsonPath("$[0].idTransaccionExterna").doesNotExist())
                .andExpect(jsonPath("$[0].intentosPago").doesNotExist())
                .andExpect(jsonPath("$[0].length()").value(12));
    }

    @Test
    void sinParametrosNoFiltraYDejaLaPaginacionAlCasoDeUso() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(2));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))).andExpect(status().isOk());

        assertEquals(new FiltroHistorialPagos(null, null, null, null, null, null, null, null), verificarLlamada().getValue());
    }

    @Test
    void traduceTodosLosParametrosAlFiltro() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(2));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("estado", "pagado").param("metodo", "EN_LINEA").param("clienteId", "4").param("tecnicoId", "7")
                        .param("desde", "2030-05-01").param("hasta", "2030-05-31").param("page", "2").param("size", "50"))
                .andExpect(status().isOk());

        assertEquals(new FiltroHistorialPagos(EstadoPago.PAGADO, MetodoPago.EN_LINEA, 4L, 7L,
                LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31), 2, 50), verificarLlamada().getValue());
    }

    @Test
    void estadoOMetodoDesconocidosSon400ConProblemDetail() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("estado", "COBRADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Valor inválido para estado: COBRADO"))
                .andExpect(jsonPath("$.instance").value(RUTA));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("metodo", "TARJETA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para metodo: TARJETA"));
        verifyNoInteractions(consultar);
    }

    @Test
    void fechaMalEscritaEs400() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("desde", "01/05/2030"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("desde debe tener el formato yyyy-MM-dd"));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("hasta", "mañana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("hasta debe tener el formato yyyy-MM-dd"));
        verifyNoInteractions(consultar);
    }

    @Test
    void reglasDelCasoDeUsoComoRangoInvertidoOPaginaInvalidaSon400() throws Exception {
        when(consultar.consultar(any()))
                .thenThrow(new FiltroCitasInvalidoException("desde no puede ser posterior a hasta"))
                .thenThrow(new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y 100"))
                .thenThrow(new FiltroCitasInvalidoException("La página debe ser mayor o igual a 0"));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2030-06-01").param("hasta", "2030-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("desde no puede ser posterior a hasta"));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El tamaño debe estar entre 1 y 100"));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La página debe ser mayor o igual a 0"));
    }

    @Test
    void valoresQueNoSonNumerosSon400SinLlegarAlCasoDeUso() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("page", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("clienteId", "uno"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultar);
    }

    @Test
    void clienteYTecnicoRecibenElMismo403YSinTokenEs401() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA).header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultar);
    }

    @Test
    void elEncabezadoTotalSigueExpuestoALosNavegadoresDelFrontend() throws Exception {
        when(consultar.consultar(any())).thenReturn(dosPagos(2));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Total-Count"));
    }
}
