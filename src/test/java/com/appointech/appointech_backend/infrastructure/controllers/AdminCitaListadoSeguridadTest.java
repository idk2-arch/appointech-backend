package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/admin/citas con la seguridad real (filtro JWT + reglas de SecurityConfig). */
@WebMvcTest(value = AdminCitaController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminCitaListadoSeguridadTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 7, 6, 8, 30);

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }
    @MockitoBean
    private ConsultarCitasAdminUseCase consultarCitas;
    @MockitoBean
    private ConsultarDetalleCitaAdminUseCase consultarDetalle;
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

    private PaginaResultado<CitaResumenAdmin> pagina() {
        return new PaginaResultado<>(List.of(
                new CitaResumenAdmin(10L, INICIO, INICIO.plusHours(2), EstadoCita.CONFIRMADA,
                        "Ana Gómez", "Luis Pérez", "Plomería", "Calle 1"),
                new CitaResumenAdmin(11L, INICIO.minusDays(1), INICIO.minusDays(1).plusHours(1), EstadoCita.PENDIENTE,
                        "Ana Gómez", null, "Electricidad", "Calle 2")), 1, 2, 7, 4);
    }

    @Test
    void administradorRecibe200ConElContratoExacto() throws Exception {
        when(consultarCitas.consultar(any())).thenReturn(pagina());

        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(2))
                .andExpect(jsonPath("$.contenido[0].id").value(10))
                .andExpect(jsonPath("$.contenido[0].fecha").value("2026-07-06"))
                .andExpect(jsonPath("$.contenido[0].horaInicio").value("08:30"))
                .andExpect(jsonPath("$.contenido[0].horaFin").value("10:30"))
                .andExpect(jsonPath("$.contenido[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.contenido[0].nombreCliente").value("Ana Gómez"))
                .andExpect(jsonPath("$.contenido[0].nombreTecnico").value("Luis Pérez"))
                .andExpect(jsonPath("$.contenido[0].servicio").value("Plomería"))
                .andExpect(jsonPath("$.contenido[0].direccion").value("Calle 1"))
                .andExpect(jsonPath("$.contenido[1].nombreTecnico").doesNotExist())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(2))
                .andExpect(jsonPath("$.totalElementos").value(7))
                .andExpect(jsonPath("$.totalPaginas").value(4));
    }

    @Test
    void sinParametrosUsaPagina0Tamanio10YSinFiltros() throws Exception {
        when(consultarCitas.consultar(any())).thenReturn(pagina());

        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroCitasAdmin> captor = ArgumentCaptor.forClass(FiltroCitasAdmin.class);
        verify(consultarCitas).consultar(captor.capture());
        assertEquals(new FiltroCitasAdmin(null, null, null, null, 0, 10), captor.getValue());
    }

    @Test
    void pasaTodosLosFiltrosYLaPaginacionAlCasoDeUso() throws Exception {
        when(consultarCitas.consultar(any())).thenReturn(pagina());

        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR"))
                        .param("tecnicoId", "5").param("estado", "en_camino")
                        .param("fechaDesde", "2026-07-01").param("fechaHasta", "2026-07-31")
                        .param("pagina", "2").param("tamanio", "50"))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroCitasAdmin> captor = ArgumentCaptor.forClass(FiltroCitasAdmin.class);
        verify(consultarCitas).consultar(captor.capture());
        assertEquals(new FiltroCitasAdmin(5L, EstadoCita.EN_CAMINO, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), 2, 50), captor.getValue());
    }

    @Test
    void estadoInvalidoResponde400ConProblemDetail() throws Exception {
        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR")).param("estado", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Estado inválido: XYZ"))
                .andExpect(jsonPath("$.instance").value("/api/admin/citas"));
        verifyNoInteractions(consultarCitas);
    }

    @Test
    void fechaConFormatoInvalidoResponde400() throws Exception {
        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR"))
                        .param("fechaDesde", "01/07/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("fechaDesde debe tener el formato yyyy-MM-dd"));
        verifyNoInteractions(consultarCitas);
    }

    @Test
    void reglasDelCasoDeUsoComoRangoDeFechasRespondenFormato400() throws Exception {
        when(consultarCitas.consultar(any())).thenThrow(
                new com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException("fechaDesde no puede ser posterior a fechaHasta"));

        mvc.perform(get("/api/admin/citas").header("Authorization", token("ADMINISTRADOR"))
                        .param("fechaDesde", "2026-08-01").param("fechaHasta", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("fechaDesde no puede ser posterior a fechaHasta"));
    }

    @Test
    void tecnicoYClienteReciben403() throws Exception {
        mvc.perform(get("/api/admin/citas").header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/citas").header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(consultarCitas);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/admin/citas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarCitas);
    }
}
