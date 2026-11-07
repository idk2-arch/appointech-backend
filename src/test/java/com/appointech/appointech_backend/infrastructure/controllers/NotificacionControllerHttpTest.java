package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.NotificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.TipoNotificacion;
import com.appointech.appointech_backend.domain.ports.in.ConsultarNotificacionesUseCase;
import com.appointech.appointech_backend.domain.ports.in.MarcarNotificacionesLeidasUseCase;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/notificaciones con la seguridad real (JWT) y el manejador de errores real: cada rol ve solo lo suyo. */
@WebMvcTest(value = NotificacionController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class NotificacionControllerHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/notificaciones";

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private ConsultarNotificacionesUseCase consultar;
    @MockitoBean
    private MarcarNotificacionesLeidasUseCase marcar;
    @MockitoBean
    private com.appointech.appointech_backend.domain.ports.out.RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String correo, String rol) {
        return "Bearer " + Jwts.builder().subject(correo).claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static Notificacion notificacion(long id, boolean leida) {
        return new Notificacion(id, 11L, TipoNotificacion.CITA_FINALIZADA, "Servicio finalizado",
                "Tu cita #5 finalizó. Ya puedes calificar el servicio.", 5L, leida, LocalDateTime.of(2030, 5, 2, 10, 0));
    }

    @Test
    void cualquierRolListaSusNotificacionesConElTotalEnElEncabezado() throws Exception {
        when(consultar.listar(eq("ana@x.com"), anyBoolean(), any()))
                .thenReturn(new ListadoConTotal<>(List.of(notificacion(2, false), notificacion(1, true)), 37));

        for (String rol : List.of("CLIENTE", "TECNICO", "ADMINISTRADOR")) {
            mvc.perform(get(RUTA).header("Authorization", token("ana@x.com", rol)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "37"))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(2))
                    .andExpect(jsonPath("$[0].tipo").value("CITA_FINALIZADA"))
                    .andExpect(jsonPath("$[0].titulo").value("Servicio finalizado"))
                    .andExpect(jsonPath("$[0].mensaje").value("Tu cita #5 finalizó. Ya puedes calificar el servicio."))
                    .andExpect(jsonPath("$[0].citaId").value(5))
                    .andExpect(jsonPath("$[0].leida").value(false))
                    .andExpect(jsonPath("$[0].creadaEn").exists())
                    .andExpect(jsonPath("$[1].leida").value(true));
        }
    }

    @Test
    void elFiltroYLaPaginacionLlegaranAlCasoDeUsoParaElUsuarioDelToken() throws Exception {
        when(consultar.listar(any(), anyBoolean(), any())).thenReturn(new ListadoConTotal<>(List.of(), 0));

        mvc.perform(get(RUTA).header("Authorization", token("ana@x.com", "CLIENTE"))
                        .param("soloNoLeidas", "TRUE").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$.length()").value(0));

        ArgumentCaptor<PaginacionOpcional> pagina = ArgumentCaptor.forClass(PaginacionOpcional.class);
        verify(consultar).listar(eq("ana@x.com"), eq(true), pagina.capture());
        assertEquals(new PaginacionOpcional(2, 5), pagina.getValue());
    }

    @Test
    void sinParametrosNoFiltraYNoPidePaginacionExplicita() throws Exception {
        when(consultar.listar(any(), anyBoolean(), any())).thenReturn(new ListadoConTotal<>(List.of(), 0));

        mvc.perform(get(RUTA).header("Authorization", token("ana@x.com", "TECNICO"))).andExpect(status().isOk());

        ArgumentCaptor<PaginacionOpcional> pagina = ArgumentCaptor.forClass(PaginacionOpcional.class);
        verify(consultar).listar(eq("ana@x.com"), eq(false), pagina.capture());
        assertEquals(new PaginacionOpcional(null, null), pagina.getValue());
    }

    @Test
    void parametrosInvalidosSon400ConProblemDetail() throws Exception {
        String autorizacion = token("ana@x.com", "CLIENTE");

        mvc.perform(get(RUTA).header("Authorization", autorizacion).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El tamaño debe estar entre 1 y 100"))
                .andExpect(jsonPath("$.instance").value(RUTA));
        mvc.perform(get(RUTA).header("Authorization", autorizacion).param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA).header("Authorization", autorizacion).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La página debe ser mayor o igual a 0"));
        mvc.perform(get(RUTA).header("Authorization", autorizacion).param("soloNoLeidas", "quizas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para soloNoLeidas: quizas"));
        verifyNoInteractions(consultar);
    }

    @Test
    void elConteoDeNoLeidasEsUnObjetoConElTotal() throws Exception {
        when(consultar.contarNoLeidas("ana@x.com")).thenReturn(4L);

        mvc.perform(get(RUTA + "/no-leidas/conteo").header("Authorization", token("ana@x.com", "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void marcarUnaComoLeidaDevuelveLaNotificacionActualizada() throws Exception {
        when(marcar.marcarLeida("ana@x.com", 8L)).thenReturn(notificacion(8, true));

        mvc.perform(patch(RUTA + "/8/leida").header("Authorization", token("ana@x.com", "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    void marcarUnaAjenaOInexistenteEs404ConProblemDetail() throws Exception {
        when(marcar.marcarLeida("ana@x.com", 99L)).thenThrow(new NotificacionNoEncontradaException(99L));

        mvc.perform(patch(RUTA + "/99/leida").header("Authorization", token("ana@x.com", "CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("La notificación 99 no existe"))
                .andExpect(jsonPath("$.instance").value(RUTA + "/99/leida"));
    }

    @Test
    void leerTodasRespondeCuantasActualizo() throws Exception {
        when(marcar.marcarTodasLeidas("ana@x.com")).thenReturn(6);

        mvc.perform(patch(RUTA + "/leer-todas").header("Authorization", token("ana@x.com", "TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(6));
    }

    @Test
    void sinTokenTodoEs401() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA + "/no-leidas/conteo")).andExpect(status().isUnauthorized());
        mvc.perform(patch(RUTA + "/1/leida")).andExpect(status().isUnauthorized());
        mvc.perform(patch(RUTA + "/leer-todas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultar, marcar);
    }
}
