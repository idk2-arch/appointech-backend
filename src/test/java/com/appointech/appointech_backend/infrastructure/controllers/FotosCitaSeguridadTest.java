package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.FotoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.ImagenInvalidaException;
import com.appointech.appointech_backend.domain.exception.LimiteFotosCitaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.in.AgregarFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.EliminarFotoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerFotoCitaUseCase;
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
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fotos del electrodoméstico con la seguridad real: ninguna ruta es pública y la pertenencia la decide el caso de uso. */
@WebMvcTest(value = {ClienteCitaFotoController.class, CitaFotoController.class},
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class FotosCitaSeguridadTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String CUERPO = "{\"imagenBase64\":\"data:image/png;base64,AAAA\"}";
    private static final String RUTA_CLIENTE = "/api/cliente/citas/20/fotos";
    private static final String RUTA_LECTURA = "/api/citas/20/fotos/5";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private AgregarFotoCitaUseCase agregarFoto;
    @MockitoBean
    private EliminarFotoCitaUseCase eliminarFoto;
    @MockitoBean
    private ObtenerFotoCitaUseCase obtenerFoto;
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

    // ---------- POST /api/cliente/citas/{citaId}/fotos ----------

    @Test
    void elClienteAdjuntaUnaFotoYRecibe201ConIdYUrl() throws Exception {
        when(agregarFoto.agregar("u@mail.com", 20L, "data:image/png;base64,AAAA"))
                .thenReturn(new CitaFoto(5L, 20L, "citas/20/k", LocalDateTime.now()));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.url").value("/api/citas/20/fotos/5"));
    }

    @Test
    void adjuntarSinTokenResponde401() throws Exception {
        mvc.perform(post(RUTA_CLIENTE).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(agregarFoto);
    }

    @Test
    void tecnicoYAdministradorNoPuedenAdjuntarPorLaRutaDelCliente() throws Exception {
        for (String rol : new String[]{"TECNICO", "ADMINISTRADOR"}) {
            mvc.perform(post(RUTA_CLIENTE).header("Authorization", token(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(agregarFoto);
    }

    @Test
    void otroClienteRecibe403() throws Exception {
        when(agregarFoto.agregar(any(), any(), any())).thenThrow(new AccesoCitaDenegadoException());

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unaImagenInvalidaOUnCuerpoVacioOGrandeResponden400() throws Exception {
        when(agregarFoto.agregar(any(), any(), any())).thenThrow(new ImagenInvalidaException("Formato no permitido"));
        char[] enorme = new char[1_500_001];
        Arrays.fill(enorme, 'A');

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato no permitido"));
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imagenBase64\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagenBase64\":\"" + new String(enorme) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void superarElLimiteDeTresFotosResponde409() throws Exception {
        when(agregarFoto.agregar(any(), any(), any())).thenThrow(new LimiteFotosCitaException(3));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La cita ya tiene el máximo de 3 fotos"));
    }

    @Test
    void unaCitaFueraDeEstadoResponde409YUnaCitaInexistenteResponde404() throws Exception {
        when(agregarFoto.agregar(any(), any(), any()))
                .thenThrow(new EstadoCitaInvalidoException("Solo PENDIENTE o CONFIRMADA"))
                .thenThrow(new SolicitudNoEncontradaException(20L));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    void siElBucketNoRespondeResponde503() throws Exception {
        when(agregarFoto.agregar(any(), any(), any()))
                .thenThrow(new AlmacenamientoNoDisponibleException("no disponible", new RuntimeException()));

        mvc.perform(post(RUTA_CLIENTE).header("Authorization", token("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------- DELETE /api/cliente/citas/{citaId}/fotos/{fotoId} ----------

    @Test
    void elClienteBorraUnaFotoYRecibe204() throws Exception {
        mvc.perform(delete(RUTA_CLIENTE + "/5").header("Authorization", token("CLIENTE")))
                .andExpect(status().isNoContent());
        verify(eliminarFoto).eliminar("u@mail.com", 20L, 5L);
    }

    @Test
    void borrarSinTokenResponde401YConOtroRolResponde403() throws Exception {
        mvc.perform(delete(RUTA_CLIENTE + "/5")).andExpect(status().isUnauthorized());
        mvc.perform(delete(RUTA_CLIENTE + "/5").header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        verifyNoInteractions(eliminarFoto);
    }

    @Test
    void borrarUnaFotoAjenaOInexistenteResponde403Y404() throws Exception {
        org.mockito.Mockito.doThrow(new AccesoCitaDenegadoException())
                .doThrow(new FotoNoEncontradaException())
                .when(eliminarFoto).eliminar(any(), any(), any());

        mvc.perform(delete(RUTA_CLIENTE + "/5").header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(delete(RUTA_CLIENTE + "/5").header("Authorization", token("CLIENTE"))).andExpect(status().isNotFound());
    }

    // ---------- GET /api/citas/{citaId}/fotos/{fotoId} ----------

    @Test
    void conSesionSeObtienenLosBytesConTipoYCachePrivado() throws Exception {
        byte[] bytes = {(byte) 0x89, 'P', 'N', 'G'};
        when(obtenerFoto.obtener("u@mail.com", 20L, 5L)).thenReturn(new FotoAlmacenada(bytes, "image/png"));

        for (String rol : new String[]{"CLIENTE", "TECNICO", "ADMINISTRADOR"}) {
            byte[] cuerpo = mvc.perform(get(RUTA_LECTURA).header("Authorization", token(rol)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(header().string("Cache-Control", containsString("private")))
                    .andReturn().getResponse().getContentAsByteArray();
            assertArrayEquals(bytes, cuerpo);
        }
    }

    @Test
    void leerSinTokenResponde401YNuncaEsPublica() throws Exception {
        mvc.perform(get(RUTA_LECTURA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(obtenerFoto);
    }

    @Test
    void otroClienteOTecnicoRecibe403() throws Exception {
        when(obtenerFoto.obtener(any(), any(), any())).thenThrow(new AccesoCitaDenegadoException());

        mvc.perform(get(RUTA_LECTURA).header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_LECTURA).header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
    }

    @Test
    void unaFotoInexistenteResponde404() throws Exception {
        when(obtenerFoto.obtener(any(), any(), any())).thenThrow(new FotoNoEncontradaException());

        mvc.perform(get(RUTA_LECTURA).header("Authorization", token("CLIENTE"))).andExpect(status().isNotFound());
    }
}
