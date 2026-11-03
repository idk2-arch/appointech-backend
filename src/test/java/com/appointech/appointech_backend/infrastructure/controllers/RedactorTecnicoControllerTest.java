package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.RedaccionTecnica;
import com.appointech.appointech_backend.domain.ports.in.RedactarDescripcionTecnicaUseCase;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/solicitudes/redactar-tecnico con la seguridad real (JWT y rol CLIENTE). */
@WebMvcTest(value = RedactorTecnicoController.class,
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class RedactorTecnicoControllerTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/solicitudes/redactar-tecnico";
    private static final String CUERPO = "{\"descripcionProblema\":\"se me sale el agua\",\"especialidad\":\"Plomería\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private RedactarDescripcionTecnicaUseCase redactar;
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
    void elClienteRecibeElTextoTecnico() throws Exception {
        when(redactar.redactar("se me sale el agua", "Plomería"))
                .thenReturn(RedaccionTecnica.conTexto("Fuga de agua en conexión."));

        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.textoTecnico").value("Fuga de agua en conexión."));
    }

    @Test
    void sinServicioDeIAResponde200ConElMensajeNeutro() throws Exception {
        when(redactar.redactar("se me sale el agua", null)).thenReturn(RedaccionTecnica.noDisponible());

        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionProblema\":\"se me sale el agua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.textoTecnico").value(RedaccionTecnica.MENSAJE_NO_DISPONIBLE));
    }

    @Test
    void sinTokenResponde401YConOtroRolResponde403() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        for (String rol : new String[]{"TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(post(RUTA).header("Authorization", token(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(redactar);
    }

    @Test
    void descripcionVaciaOMuyLargaOEspecialidadLargaResponde400() throws Exception {
        for (String cuerpo : new String[]{"{}", "{\"descripcionProblema\":\"  \"}",
                "{\"descripcionProblema\":\"" + "a".repeat(1001) + "\"}"}) {
            mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(startsWith("descripcionProblema:")));
        }
        mvc.perform(post(RUTA).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionProblema\":\"falla\",\"especialidad\":\"" + "e".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("especialidad:")));
        verifyNoInteractions(redactar);
    }
}
