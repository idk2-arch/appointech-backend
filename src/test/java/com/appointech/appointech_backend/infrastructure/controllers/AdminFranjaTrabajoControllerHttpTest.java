package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FranjaTrabajoConflictoException;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.in.AdministrarFranjasTrabajoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CambiarRadioAtencionUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarFranjasTrabajoUseCase;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Franjas de trabajo y radio de atención con la seguridad real (JWT y roles) y el manejador de errores real. */
@WebMvcTest(value = AdminFranjaTrabajoController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminFranjaTrabajoControllerHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String BASE = "/api/admin/tecnicos/1";
    private static final String CUERPO = "{\"fecha\":\"2030-05-03\",\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\"}";
    private static final LocalDate FECHA = LocalDate.of(2030, 5, 3);

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private AdministrarFranjasTrabajoUseCase administrar;
    @MockitoBean
    private ConsultarFranjasTrabajoUseCase consultar;
    @MockitoBean
    private CambiarRadioAtencionUseCase cambiarRadio;
    @MockitoBean
    private com.appointech.appointech_backend.domain.ports.out.RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String rol) {
        return "Bearer " + Jwts.builder().subject("admin@appointech.com").claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static FranjaTrabajoConOcupacion franja(long id, int ocupados) {
        return new FranjaTrabajoConOcupacion(new FranjaTrabajo(id, 1L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0),
                77L, LocalDateTime.of(2030, 5, 2, 10, 0)), 2, ocupados);
    }

    // ---------- crear ----------

    @Test
    void crearRespondeCreated201ConLaFranjaYSusBloques() throws Exception {
        when(administrar.crear("admin@appointech.com", 1L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0)))
                .thenReturn(franja(5, 0));

        mvc.perform(post(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tecnicoId").value(1))
                .andExpect(jsonPath("$.fecha").value("2030-05-03"))
                .andExpect(jsonPath("$.horaInicio").value("08:00"))
                .andExpect(jsonPath("$.horaFin").value("12:00"))
                .andExpect(jsonPath("$.bloques").value(2))
                .andExpect(jsonPath("$.bloquesOcupados").value(0))
                .andExpect(jsonPath("$.creadaEn").exists());
    }

    @Test
    void crearConFormatoMalEscritoEs400SinLlegarAlCasoDeUso() throws Exception {
        String[] cuerpos = {
                "{\"fecha\":\"03/05/2030\",\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\"}",
                "{\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\"}",
                "{\"fecha\":\"2030-05-03\",\"horaInicio\":\"8am\",\"horaFin\":\"12:00\"}",
                "{\"fecha\":\"2030-05-03\",\"horaInicio\":\"08:00\"}"};
        for (String cuerpo : cuerpos) {
            mvc.perform(post(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.instance").value(BASE + "/franjas"));
        }
        verifyNoInteractions(administrar);
    }

    @Test
    void crearConDatosInvalidosEs400ConElMensajeDelDominio() throws Exception {
        when(administrar.crear(any(), any(), any(), any(), any()))
                .thenThrow(new FranjaTrabajoInvalidaException("La hora de inicio debe ser anterior a la hora de fin"));

        mvc.perform(post(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La hora de inicio debe ser anterior a la hora de fin"));
    }

    @Test
    void crearConChoqueEs409ConElDetalleDelChoque() throws Exception {
        when(administrar.crear(any(), any(), any(), any(), any())).thenThrow(new FranjaTrabajoConflictoException(
                "La franja se cruza con una no disponibilidad del técnico (del 2030-05-03 09:00 al 2030-05-03 10:30); no se creó la franja"));

        mvc.perform(post(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("2030-05-03 09:00 al 2030-05-03 10:30")));
    }

    @Test
    void crearConTecnicoInexistenteEs404() throws Exception {
        when(administrar.crear(any(), any(), any(), any(), any())).thenThrow(new TecnicoNoEncontradoException(1L));

        mvc.perform(post(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    // ---------- listar ----------

    @Test
    void listarSinFechasPideElRangoPorDefectoYDevuelveUnArray() throws Exception {
        when(consultar.listar(1L, null, null)).thenReturn(List.of(franja(5, 1), franja(6, 0)));

        mvc.perform(get(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].bloques").value(2))
                .andExpect(jsonPath("$[0].bloquesOcupados").value(1))
                .andExpect(jsonPath("$[1].bloquesOcupados").value(0));
    }

    @Test
    void listarPasaLasFechasYLasMalEscritasSon400() throws Exception {
        when(consultar.listar(1L, FECHA, FECHA.plusDays(10))).thenReturn(List.of());

        mvc.perform(get(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2030-05-03").param("hasta", "2030-05-13"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR")).param("desde", "mañana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("desde debe tener el formato yyyy-MM-dd"));
    }

    @Test
    void listarConRangoMayorA90DiasEs400() throws Exception {
        when(consultar.listar(any(), any(), any())).thenThrow(new FiltroCitasInvalidoException("El rango no puede superar 90 días"));

        mvc.perform(get(BASE + "/franjas").header("Authorization", token("ADMINISTRADOR"))
                        .param("desde", "2030-01-01").param("hasta", "2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El rango no puede superar 90 días"));
    }

    // ---------- cambiar y borrar ----------

    @Test
    void actualizarRespondeOkConLaFranjaCambiada() throws Exception {
        when(administrar.actualizar("admin@appointech.com", 1L, 5L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0)))
                .thenReturn(franja(5, 0));

        mvc.perform(put(BASE + "/franjas/5").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void actualizarOBorrarConCitasEs409YUnaFranjaInexistenteEs404() throws Exception {
        when(administrar.actualizar(any(), any(), eq(5L), any(), any(), any())).thenThrow(new FranjaTrabajoConflictoException(
                "No se puede cambiar la franja 2030-05-03 de 08:00 a 12:00: ya tiene 2 citas agendadas. Cancélalas o reasígnalas antes"));
        when(administrar.actualizar(any(), any(), eq(404L), any(), any(), any())).thenThrow(new FranjaTrabajoNoEncontradaException(404L));

        mvc.perform(put(BASE + "/franjas/5").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ya tiene 2 citas")));
        mvc.perform(put(BASE + "/franjas/404").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrarRespondeSinContenidoYSiNoExisteEs404() throws Exception {
        mvc.perform(delete(BASE + "/franjas/5").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isNoContent());
        verify(administrar).eliminar("admin@appointech.com", 1L, 5L);

        org.mockito.Mockito.doThrow(new FranjaTrabajoNoEncontradaException(9L)).when(administrar).eliminar(any(), any(), eq(9L));
        mvc.perform(delete(BASE + "/franjas/9").header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    // ---------- radio de atención ----------

    @Test
    void cambiarElRadioDevuelveElRadioDelTecnico() throws Exception {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(1L);
        tecnico.setRadioCoberturaKm(30);
        when(cambiarRadio.cambiar("admin@appointech.com", 1L, 30)).thenReturn(tecnico);

        mvc.perform(patch(BASE + "/radio-atencion").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"radioMaximoKm\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoId").value(1))
                .andExpect(jsonPath("$.radioMaximoKm").value(30));
    }

    @Test
    void unRadioInvalidoEs400() throws Exception {
        when(cambiarRadio.cambiar(any(), any(), any()))
                .thenThrow(new RadioCoberturaInvalidoException("El radio de atención debe estar entre 1 y 100 km"));

        mvc.perform(patch(BASE + "/radio-atencion").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"radioMaximoKm\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El radio de atención debe estar entre 1 y 100 km"));
    }

    // ---------- seguridad ----------

    @Test
    void clienteYTecnicoRecibenElMismo403YSinTokenEs401EnTodosLosEndpoints() throws Exception {
        for (String rol : List.of("CLIENTE", "TECNICO")) {
            String autorizacion = token(rol);
            mvc.perform(post(BASE + "/franjas").header("Authorization", autorizacion)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andExpect(status().isForbidden());
            mvc.perform(get(BASE + "/franjas").header("Authorization", autorizacion)).andExpect(status().isForbidden());
            mvc.perform(put(BASE + "/franjas/5").header("Authorization", autorizacion)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO)).andExpect(status().isForbidden());
            mvc.perform(delete(BASE + "/franjas/5").header("Authorization", autorizacion)).andExpect(status().isForbidden());
            mvc.perform(patch(BASE + "/radio-atencion").header("Authorization", autorizacion)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"radioMaximoKm\":30}")).andExpect(status().isForbidden());
        }
        mvc.perform(get(BASE + "/franjas")).andExpect(status().isUnauthorized());
        mvc.perform(patch(BASE + "/radio-atencion").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(administrar, consultar, cambiarRadio);
    }
}
