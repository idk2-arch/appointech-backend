package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoPerfilUseCase;
import com.appointech.appointech_backend.domain.ports.in.ActualizarFotoUsuarioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoUseCase;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de fotos con la seguridad real (filtro JWT y reglas de SecurityConfig). */
@WebMvcTest(value = {FotoPerfilController.class, AdminFotoUsuarioController.class, FotoController.class},
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class FotosSeguridadTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String CLAVE = "11111111-2222-3333-4444-555555555555";
    private static final String CUERPO = "{\"imagenBase64\":\"data:image/png;base64,AAAA\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private ActualizarFotoPerfilUseCase actualizarPerfil;
    @MockitoBean
    private ActualizarFotoUsuarioUseCase actualizarUsuario;
    @MockitoBean
    private ObtenerFotoUseCase obtenerFoto;
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

    // ---------- PUT /api/perfil/foto ----------

    @Test
    void cualquierRolGuardaSuFotoYRecibeLaUrl() throws Exception {
        when(actualizarPerfil.reemplazar("u@mail.com", "data:image/png;base64,AAAA")).thenReturn(CLAVE);

        for (String rol : new String[]{"CLIENTE", "TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(put("/api/perfil/foto").header("Authorization", token(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fotoUrl").value("/api/fotos/" + CLAVE));
        }
    }

    @Test
    void guardarSinTokenResponde401() throws Exception {
        mvc.perform(put("/api/perfil/foto").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(actualizarPerfil);
    }

    @Test
    void unaImagenInvalidaResponde400ConProblemDetail() throws Exception {
        when(actualizarPerfil.reemplazar(any(), any())).thenThrow(new ImagenInvalidaException("Formato no permitido"));

        mvc.perform(put("/api/perfil/foto").header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Formato no permitido"))
                .andExpect(jsonPath("$.instance").value("/api/perfil/foto"));
    }

    @Test
    void siFaltaLaImagenResponde400() throws Exception {
        mvc.perform(put("/api/perfil/foto").header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imagenBase64\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("imagenBase64:")));
        verifyNoInteractions(actualizarPerfil);
    }

    @Test
    void unCuerpoDemasiadoGrandeSeRechazaConUn400SinLlegarAlCasoDeUso() throws Exception {
        char[] enorme = new char[1_500_001];
        Arrays.fill(enorme, 'A');

        mvc.perform(put("/api/perfil/foto").header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagenBase64\":\"" + new String(enorme) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(actualizarPerfil);
    }

    @Test
    void siElBucketNoRespondeResponde503() throws Exception {
        when(actualizarPerfil.reemplazar(any(), any()))
                .thenThrow(new AlmacenamientoNoDisponibleException("no disponible", new RuntimeException()));

        mvc.perform(put("/api/perfil/foto").header("Authorization", token("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // ---------- DELETE /api/perfil/foto ----------

    @Test
    void borrarLaFotoPropiaResponde204() throws Exception {
        mvc.perform(delete("/api/perfil/foto").header("Authorization", token("TECNICO")))
                .andExpect(status().isNoContent());
        verify(actualizarPerfil).eliminar("u@mail.com");
    }

    @Test
    void borrarSinTokenResponde401() throws Exception {
        mvc.perform(delete("/api/perfil/foto")).andExpect(status().isUnauthorized());
        verifyNoInteractions(actualizarPerfil);
    }

    // ---------- PUT /api/admin/usuarios/{id}/foto ----------

    @Test
    void elAdministradorGuardaLaFotoDeOtroUsuario() throws Exception {
        when(actualizarUsuario.reemplazar(5L, "data:image/png;base64,AAAA")).thenReturn(CLAVE);

        mvc.perform(put("/api/admin/usuarios/5/foto").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("/api/fotos/" + CLAVE));
    }

    @Test
    void clienteYTecnicoNoPuedenCambiarLaFotoDeOtros() throws Exception {
        for (String rol : new String[]{"CLIENTE", "TECNICO"}) {
            mvc.perform(put("/api/admin/usuarios/5/foto").header("Authorization", token(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(actualizarUsuario);
    }

    @Test
    void cambiarLaFotoDeOtroSinTokenResponde401() throws Exception {
        mvc.perform(put("/api/admin/usuarios/5/foto").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioInexistenteResponde404() throws Exception {
        when(actualizarUsuario.reemplazar(any(), any())).thenThrow(new UsuarioNoEncontradoException(99L));

        mvc.perform(put("/api/admin/usuarios/99/foto").header("Authorization", token("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /api/fotos/{clave} (público) ----------

    @Test
    void cualquieraPuedeVerUnaFotoSinTokenConTipoYCache() throws Exception {
        byte[] bytes = {(byte) 0x89, 'P', 'N', 'G'};
        when(obtenerFoto.obtener(CLAVE)).thenReturn(new FotoAlmacenada(bytes, "image/png"));

        byte[] cuerpo = mvc.perform(get("/api/fotos/" + CLAVE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(bytes, cuerpo);
    }

    @Test
    void unaFotoInexistenteResponde404() throws Exception {
        when(obtenerFoto.obtener(CLAVE)).thenThrow(new FotoNoEncontradaException());

        mvc.perform(get("/api/fotos/" + CLAVE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void elPermisoPublicoSoloCubreElGetDeUnaClave() throws Exception {
        mvc.perform(post("/api/fotos/" + CLAVE)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/fotos/" + CLAVE)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/fotos/" + CLAVE + "/otra")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil/otra")).andExpect(status().isUnauthorized());
    }
}
