package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.CalificacionInvalidaException;
import com.appointech.appointech_backend.domain.exception.CalificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.CitaNoFinalizadaException;
import com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.PlazoCalificacionVencidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.CalificacionesDelTecnico;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.in.CalificarServicioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.AgendamientoExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.SecurityConfig;
import com.appointech.appointech_backend.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de calificaciones con la seguridad real (JWT y roles) y los manejadores de error reales. */
@WebMvcTest(value = {ClienteCalificacionController.class, TecnicoCalificacionController.class, AdminCitaController.class},
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, AgendamientoExceptionHandler.class})
class CalificacionesHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA_CLIENTE = "/api/cliente/citas/10/calificacion";
    private static final String RUTA_TECNICO = "/api/tecnico/calificaciones";
    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 12, 0);
    private static final String CUERPO_VALIDO = "{\"puntuacion\":5,\"comentario\":\"Excelente\"}";

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private CalificarServicioUseCase calificar;
    @MockitoBean
    private ConsultarCalificacionUseCase consultar;
    @MockitoBean
    private ConsultarCalificacionesTecnicoUseCase consultarDelTecnico;
    @MockitoBean
    private ConsultarCitasAdminUseCase consultarCitasAdmin;
    @MockitoBean
    private ConsultarDetalleCitaAdminUseCase consultarDetalleAdmin;
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

    // ---------- POST /api/cliente/citas/{id}/calificacion ----------

    @Test
    void elClienteCalificaYRecibe201ConLaCalificacionCreada() throws Exception {
        when(calificar.calificar(any(), eq(10L), eq(5), eq("Excelente")))
                .thenReturn(new Calificacion(1L, 10L, 5, "Excelente", FECHA));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citaId").value(10))
                .andExpect(jsonPath("$.puntuacion").value(5))
                .andExpect(jsonPath("$.comentario").value("Excelente"))
                .andExpect(jsonPath("$.fecha").value("2030-06-01T12:00:00"));
    }

    @Test
    void elComentarioEsOpcional() throws Exception {
        when(calificar.calificar(any(), eq(10L), eq(3), eq(null)))
                .thenReturn(new Calificacion(1L, 10L, 3, null, FECHA));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType("application/json").content("{\"puntuacion\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comentario").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void unaPuntuacionOUnComentarioInvalidosResponden400ConMensaje() throws Exception {
        when(calificar.calificar(any(), any(), any(), any()))
                .thenThrow(new CalificacionInvalidaException("La puntuación debe ser un número entero entre 1 y 5"));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType("application/json").content("{\"puntuacion\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La puntuación debe ser un número entero entre 1 y 5"));
    }

    @Test
    void laCitaDeOtroClienteOInexistenteResponde404() throws Exception {
        when(calificar.calificar(any(), any(), any(), any())).thenThrow(new SolicitudNoEncontradaException(10L));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void citaNoFinalizadaYaCalificadaYPlazoVencidoRespondenCada409ConSuMensaje() throws Exception {
        when(calificar.calificar(any(), any(), any(), any()))
                .thenThrow(new CitaNoFinalizadaException("CONFIRMADA"))
                .thenThrow(new CitaYaCalificadaException())
                .thenThrow(new PlazoCalificacionVencidoException(30));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE")).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Solo puedes calificar una cita finalizada (estado actual: CONFIRMADA)"));
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE")).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Esta cita ya fue calificada"));
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE")).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El plazo de 30 días para calificar esta cita ya venció"));
    }

    @Test
    void calificarSoloPuedeElClienteYSinTokenEs401() throws Exception {
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("TECNICO"))
                .contentType("application/json").content(CUERPO_VALIDO)).andExpect(status().isForbidden());
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("ADMINISTRADOR"))
                .contentType("application/json").content(CUERPO_VALIDO)).andExpect(status().isForbidden());
        mvc.perform(post(RUTA_CLIENTE).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(calificar);
    }

    // ---------- GET /api/cliente/citas/{id}/calificacion ----------

    @Test
    void elClienteConsultaSuCalificacionCon200() throws Exception {
        when(consultar.consultar(any(), eq(10L))).thenReturn(new Calificacion(1L, 10L, 4, "Bien", FECHA));

        mvc.perform(get(RUTA_CLIENTE).header("Authorization", token("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citaId").value(10))
                .andExpect(jsonPath("$.puntuacion").value(4))
                .andExpect(jsonPath("$.comentario").value("Bien"));
    }

    @Test
    void sinCalificacionTodaviaResponde404() throws Exception {
        when(consultar.consultar(any(), any())).thenThrow(new CalificacionNoEncontradaException(10L));

        mvc.perform(get(RUTA_CLIENTE).header("Authorization", token("CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La cita 10 aún no tiene calificación"));
    }

    @Test
    void consultarLaCalificacionDeUnaCitaAjenaResponde404() throws Exception {
        when(consultar.consultar(any(), any())).thenThrow(new SolicitudNoEncontradaException(10L));

        mvc.perform(get(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))).andExpect(status().isNotFound());
    }

    @Test
    void consultarLaCalificacionSoloPuedeElClienteYSinTokenEs401() throws Exception {
        mvc.perform(get(RUTA_CLIENTE).header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_CLIENTE).header("Authorization", token("ADMINISTRADOR"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_CLIENTE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultar);
    }

    // ---------- GET /api/tecnico/calificaciones ----------

    @Test
    void elTecnicoVeSuPromedioElTotalYSoloElNombreDePilaDelCliente() throws Exception {
        when(consultarDelTecnico.consultar(any(), eq(1), eq(10))).thenReturn(new CalificacionesDelTecnico(4.5, 12,
                List.of(new CalificacionRecibida(10L, 5, "Top", FECHA, "Ana"),
                        new CalificacionRecibida(9L, 4, null, FECHA.minusDays(1), "Luz"))));

        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("TECNICO")).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(4.5))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.calificaciones.length()").value(2))
                .andExpect(jsonPath("$.calificaciones[0].citaId").value(10))
                .andExpect(jsonPath("$.calificaciones[0].puntuacion").value(5))
                .andExpect(jsonPath("$.calificaciones[0].comentario").value("Top"))
                .andExpect(jsonPath("$.calificaciones[0].fecha").value("2030-06-01T12:00:00"))
                .andExpect(jsonPath("$.calificaciones[0].clienteNombre").value("Ana"))
                .andExpect(jsonPath("$.calificaciones[0].clienteApellido").doesNotExist())
                .andExpect(jsonPath("$.calificaciones[0].telefono").doesNotExist())
                .andExpect(jsonPath("$.calificaciones[0].correo").doesNotExist())
                .andExpect(jsonPath("$.calificaciones[1].comentario").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void sinCalificacionesElPromedioEsNuloYLaListaVacia() throws Exception {
        when(consultarDelTecnico.consultar(any(), eq(null), eq(null)))
                .thenReturn(new CalificacionesDelTecnico(null, 0, List.of()));

        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.calificaciones.length()").value(0));
    }

    @Test
    void paginaOTamanioInvalidosResponden400() throws Exception {
        when(consultarDelTecnico.consultar(any(), any(), any()))
                .thenThrow(new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y 100"));

        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("TECNICO")).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El tamaño debe estar entre 1 y 100"));
        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("TECNICO")).param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasCalificacionesDelTecnicoSoloLasVeElTecnicoYSinTokenEs401() throws Exception {
        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_TECNICO).header("Authorization", token("ADMINISTRADOR"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_TECNICO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarDelTecnico);
    }

    // ---------- GET /api/admin/citas/{id}: la calificación va al final ----------

    private static Cita citaFinalizada() {
        Cita c = new Cita();
        c.setId(10L);
        c.setEstado(EstadoCita.FINALIZADA);
        c.setEspecialidadId(5L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Calle 1");
        return c;
    }

    @Test
    void elAdministradorVeLaCalificacionEnElDetalleDeLaCita() throws Exception {
        when(consultarDetalleAdmin.detallar(10L)).thenReturn(new DetalleCitaCompleto(
                new DetalleCita(citaFinalizada(), "Luis", "Mora", "311", "Plomería"), List.of(), List.of(),
                new Calificacion(1L, 10L, 4, "Bien", FECHA)));

        mvc.perform(get("/api/admin/citas/10").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.calificacion.puntuacion").value(4))
                .andExpect(jsonPath("$.calificacion.comentario").value("Bien"))
                .andExpect(jsonPath("$.calificacion.fecha").value("2030-06-01T12:00:00"));
    }

    @Test
    void sinCalificacionElDetalleDelAdministradorTraeElCampoEnNulo() throws Exception {
        when(consultarDetalleAdmin.detallar(10L)).thenReturn(new DetalleCitaCompleto(
                new DetalleCita(citaFinalizada(), "Luis", "Mora", "311", "Plomería"), List.of()));

        mvc.perform(get("/api/admin/citas/10").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacion").value(org.hamcrest.Matchers.nullValue()));
        verify(consultarDetalleAdmin).detallar(10L);
    }
}
