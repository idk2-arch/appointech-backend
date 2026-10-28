package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.DatosPerfilEditable;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.PerfilPropio;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarPerfilPropioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPerfilPropioUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.SecurityConfig;
import com.appointech.appointech_backend.infrastructure.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET y PUT /api/perfil con la seguridad real: el usuario sale siempre del JWT. */
@WebMvcTest(value = PerfilPropioController.class,
        properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PerfilPropioControllerTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String VALIDO =
            "{\"nombre\":\"Maria\",\"apellido\":\"Lopez\",\"telefono\":\"+57 310 999 8877\",\"direccion\":\"Calle 99\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;
    @MockitoBean
    private ConsultarPerfilPropioUseCase consultar;
    @MockitoBean
    private ActualizarPerfilPropioUseCase actualizar;
    @MockitoBean
    private RegistroLogPort registroLog;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private String token(String correo) {
        return "Bearer " + Jwts.builder().subject(correo).claim("rol", "CLIENTE")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private PerfilPropio perfilTecnico() {
        Usuario u = new Usuario(2L, "Luis", "Perez", "t@mail.com", "hash", "3101234567", TipoDocumento.CC, "999",
                RolUsuario.TECNICO, true, LocalDateTime.of(2026, 1, 1, 0, 0));
        u.setFotoClave("11111111-2222-3333-4444-555555555555");
        return new PerfilPropio(u, "Carrera 2", 15, List.of(new Especialidad(1L, "Plomeria", "Tuberias", true)));
    }

    @Test
    void getDevuelveElPerfilDelUsuarioDelTokenConTodosLosCampos() throws Exception {
        when(consultar.consultar("t@mail.com")).thenReturn(perfilTecnico());

        mvc.perform(get("/api/perfil").header("Authorization", token("t@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nombre").value("Luis"))
                .andExpect(jsonPath("$.apellido").value("Perez"))
                .andExpect(jsonPath("$.correo").value("t@mail.com"))
                .andExpect(jsonPath("$.telefono").value("3101234567"))
                .andExpect(jsonPath("$.tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.numeroDocumento").value("999"))
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.direccion").value("Carrera 2"))
                .andExpect(jsonPath("$.radioCoberturaKm").value(15))
                .andExpect(jsonPath("$.especialidades[0].nombre").value("Plomeria"))
                .andExpect(jsonPath("$.fotoUrl").value("/api/fotos/11111111-2222-3333-4444-555555555555"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void getSinTokenResponde401() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultar);
    }

    @Test
    void getDeUnUsuarioInexistenteResponde404() throws Exception {
        when(consultar.consultar("x@mail.com")).thenThrow(new UsuarioNoEncontradoException("x@mail.com"));

        mvc.perform(get("/api/perfil").header("Authorization", token("x@mail.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void putActualizaUsandoSoloElCorreoDelTokenYDevuelveElPerfil() throws Exception {
        when(actualizar.actualizar(eq("c@mail.com"), any())).thenReturn(perfilTecnico());

        mvc.perform(put("/api/perfil").header("Authorization", token("c@mail.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"));

        ArgumentCaptor<DatosPerfilEditable> datos = ArgumentCaptor.forClass(DatosPerfilEditable.class);
        verify(actualizar).actualizar(eq("c@mail.com"), datos.capture());
        assertEquals(new DatosPerfilEditable("Maria", "Lopez", "+57 310 999 8877", "Calle 99"), datos.getValue());
    }

    @Test
    void losCamposNoEditablesYUnIdEnElCuerpoSeIgnoranYNuncaLlegaAlCasoDeUso() throws Exception {
        when(actualizar.actualizar(eq("c@mail.com"), any())).thenReturn(perfilTecnico());
        String conExtras = "{\"id\":99,\"correo\":\"hack@mail.com\",\"rol\":\"ADMINISTRADOR\",\"activo\":false,"
                + "\"numeroDocumento\":\"1\",\"tipoDocumento\":\"CE\",\"radioCoberturaKm\":500,"
                + "\"especialidades\":[1,2],\"nombre\":\"Maria\",\"apellido\":\"Lopez\","
                + "\"telefono\":\"3109998877\"}";

        mvc.perform(put("/api/perfil").header("Authorization", token("c@mail.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(conExtras))
                .andExpect(status().isOk());

        ArgumentCaptor<DatosPerfilEditable> datos = ArgumentCaptor.forClass(DatosPerfilEditable.class);
        verify(actualizar).actualizar(eq("c@mail.com"), datos.capture());
        assertEquals(new DatosPerfilEditable("Maria", "Lopez", "3109998877", null), datos.getValue());
    }

    @Test
    void putSinTokenResponde401() throws Exception {
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(actualizar);
    }

    @Test
    void putConDatosInvalidosResponde400ConMensajeEnEspanol() throws Exception {
        assertInvalido("{\"apellido\":\"L\",\"telefono\":\"3109998877\"}", "nombre: El nombre es obligatorio");
        assertInvalido("{\"nombre\":\"  \",\"apellido\":\"L\",\"telefono\":\"3109998877\"}",
                "nombre: El nombre es obligatorio");
        assertInvalido("{\"nombre\":\"M\",\"telefono\":\"3109998877\"}", "apellido: El apellido es obligatorio");
        assertInvalido("{\"nombre\":\"M\",\"apellido\":\"L\"}", "telefono: El teléfono es obligatorio");
        assertInvalido("{\"nombre\":\"M\",\"apellido\":\"L\",\"telefono\":\"abc123\"}",
                "telefono: El teléfono no tiene un formato válido");
        assertInvalido("{\"nombre\":\"M\",\"apellido\":\"L\",\"telefono\":\"3109998877\",\"direccion\":\"  \"}",
                "direccion: La dirección no puede estar vacía");
        assertInvalido("{\"nombre\":\"" + "a".repeat(151) + "\",\"apellido\":\"L\",\"telefono\":\"3109998877\"}",
                "nombre: El nombre no puede superar 150 caracteres");
        assertInvalido("{\"nombre\":\"M\",\"apellido\":\"L\",\"telefono\":\"3109998877\",\"direccion\":\""
                + "d".repeat(256) + "\"}", "direccion: La dirección no puede superar 255 caracteres");
        verifyNoInteractions(actualizar);
    }

    private void assertInvalido(String cuerpo, String detalle) throws Exception {
        mvc.perform(put("/api/perfil").header("Authorization", token("c@mail.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith(detalle)));
    }
}
