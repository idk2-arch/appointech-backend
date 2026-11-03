package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.PreDiagnosticoInvalidoException;
import com.appointech.appointech_backend.domain.models.PreDiagnostico;
import com.appointech.appointech_backend.domain.ports.in.GenerarPreDiagnosticoUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/solicitudes/pre-diagnostico con la seguridad real (JWT y rol CLIENTE). */
@WebMvcTest(value = PreDiagnosticoController.class,
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PreDiagnosticoControllerTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/solicitudes/pre-diagnostico";
    private static final String CUERPO = "{\"descripcionProblema\":\"Fuga bajo el lavaplatos\",\"especialidad\":\"Plomería\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private GenerarPreDiagnosticoUseCase generar;
    @MockitoBean
    private RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String rol) {
        return "Bearer " + Jwts.builder().subject("c@mail.com").claim("rol", rol)
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void elClienteRecibeLaSugerenciaConDisponibleYSugerencia() throws Exception {
        when(generar.generar("Fuga bajo el lavaplatos", "Plomería"))
                .thenReturn(PreDiagnostico.conSugerencia("Revisa el sifón."));

        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.sugerencia").value("Revisa el sifón."));
    }

    @Test
    void cuandoLaIANoEstaDisponibleSigueRespondiendo200() throws Exception {
        when(generar.generar("Fuga bajo el lavaplatos", null)).thenReturn(PreDiagnostico.noDisponible());

        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionProblema\":\"Fuga bajo el lavaplatos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.sugerencia").value(PreDiagnostico.MENSAJE_NO_DISPONIBLE));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(generar);
    }

    @Test
    void otrosRolesRecibenElForbidden403() throws Exception {
        for (String rol : new String[]{"TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(post(RUTA).header("Authorization", token(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(generar);
    }

    @Test
    void unaDescripcionVaciaOMissingResponde400() throws Exception {
        for (String cuerpo : new String[]{"{\"descripcionProblema\":\"\"}", "{\"descripcionProblema\":\"  \"}", "{}"}) {
            mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(startsWith("descripcionProblema:")));
        }
        verifyNoInteractions(generar);
    }

    @Test
    void unaDescripcionDeMasDeMilCaracteresOUnaEspecialidadLargaResponde400() throws Exception {
        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionProblema\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("descripcionProblema: La descripción del problema no puede superar 1000 caracteres"));
        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionProblema\":\"Fuga\",\"especialidad\":\"" + "e".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("especialidad: La especialidad no puede superar 100 caracteres"));
        verifyNoInteractions(generar);
    }

    @Test
    void laValidacionDelCasoDeUsoTambienSeTraduceA400() throws Exception {
        when(generar.generar("Fuga bajo el lavaplatos", "Plomería"))
                .thenThrow(new PreDiagnosticoInvalidoException("Entrada inválida"));

        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Entrada inválida"));
        verify(generar).generar("Fuga bajo el lavaplatos", "Plomería");
    }
}
