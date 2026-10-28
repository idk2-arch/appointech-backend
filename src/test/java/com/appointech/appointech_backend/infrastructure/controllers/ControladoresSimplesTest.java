package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.CredencialesInvalidasException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.CompletarPerfilUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarLogsUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearSolicitudServicioUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarEspecialidadesUseCase;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase;
import com.appointech.appointech_backend.domain.ports.in.LoginConGoogleUseCase.ResultadoLogin;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladoresSimplesTest {

    private MockMvc mvc(Object controlador) {
        return MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // ---- AuthGoogleController ----

    @Test
    void loginConGoogleExitosoDevuelveTokenPropio() throws Exception {
        LoginConGoogleUseCase caso = mock(LoginConGoogleUseCase.class);
        when(caso.ejecutar("id-token")).thenReturn(new ResultadoLogin("jwt", "CLIENTE", "Ana", "a@x.com", false, null));

        mvc(new AuthGoogleController(caso))
                .perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.token").value("jwt"))
                .andExpect(jsonPath("$.datos.perfilCompleto").value(false))
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión con Google exitoso"));
    }

    @Test
    void loginConGoogleSinTokenResponde400ConMensaje() throws Exception {
        LoginConGoogleUseCase caso = mock(LoginConGoogleUseCase.class);
        MockMvc mvc = mvc(new AuthGoogleController(caso));

        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.mensaje").value("Falta el token de Google"));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(caso);
    }

    @Test
    void loginConGoogleConTokenRechazadoResponde401() throws Exception {
        LoginConGoogleUseCase caso = mock(LoginConGoogleUseCase.class);
        when(caso.ejecutar(any())).thenThrow(new CredencialesInvalidasException());

        mvc(new AuthGoogleController(caso))
                .perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"malo\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- PerfilController ----

    @Test
    void completarPerfilUsaElCorreoDelUsuarioAutenticado() throws Exception {
        CompletarPerfilUseCase caso = mock(CompletarPerfilUseCase.class);
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("Ana");
        u.setApellido("Lopez");
        u.setCorreo("ana@mail.com");
        u.setRol(RolUsuario.CLIENTE);
        when(caso.completar("ana@mail.com", "300", TipoDocumento.CC, "55", "Calle 1")).thenReturn(u);
        Authentication auth = new UsernamePasswordAuthenticationToken("ana@mail.com", null);

        mvc(new PerfilController(caso))
                .perform(patch("/api/auth/completar-perfil").principal(auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"300\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"55\",\"direccion\":\"Calle 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Perfil completado correctamente"))
                .andExpect(jsonPath("$.datos.rol").value("CLIENTE"));
    }

    @Test
    void completarPerfilSinTelefonoResponde400() throws Exception {
        CompletarPerfilUseCase caso = mock(CompletarPerfilUseCase.class);

        mvc(new PerfilController(caso))
                .perform(patch("/api/auth/completar-perfil")
                        .principal(new UsernamePasswordAuthenticationToken("ana@mail.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"55\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(caso);
    }

    // ---- EspecialidadController ----

    @Test
    void listarEspecialidadesPublicasDevuelveSoloIdNombreDescripcion() throws Exception {
        ListarEspecialidadesUseCase caso = mock(ListarEspecialidadesUseCase.class);
        Especialidad e = new Especialidad();
        e.setId(1L);
        e.setNombre("Plomeria");
        e.setDescripcion("Tuberias");
        when(caso.listar()).thenReturn(List.of(e));

        mvc(new EspecialidadController(caso)).perform(get("/api/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Plomeria"))
                .andExpect(jsonPath("$[0].descripcion").value("Tuberias"))
                .andExpect(jsonPath("$[0].activo").doesNotExist());
    }

    // ---- LogController ----

    @Test
    void consultarLogsPasaFiltrosYLimitePorDefecto() throws Exception {
        ConsultarLogsUseCase caso = mock(ConsultarLogsUseCase.class);
        when(caso.consultar("ERROR", null, 100)).thenReturn(List.of(new RegistroLog(1L, "ERROR", "SISTEMA",
                "Job", "sys", "fallo", LocalDateTime.of(2026, 1, 1, 0, 0))));
        when(caso.consultar(null, "AUDITORIA", 5)).thenReturn(List.of());
        MockMvc mvc = mvc(new LogController(caso));

        mvc.perform(get("/api/logs").param("nivel", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mensaje").value("fallo"))
                .andExpect(jsonPath("$[0].origen").value("Job"));
        mvc.perform(get("/api/logs").param("tipo", "AUDITORIA").param("limite", "5"))
                .andExpect(jsonPath("$.length()").value(0));
        verify(caso).consultar(null, "AUDITORIA", 5);
    }

    @Test
    void logsSoloParaAdministrador() {
        PreAuthorize anotacion = LogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertTrue(anotacion.value().contains("ADMINISTRADOR"));
    }

    // ---- SolicitudServicioController ----

    @Test
    void crearSolicitudResponde201ConElDetalleDeLaCita() throws Exception {
        CrearSolicitudServicioUseCase crear = mock(CrearSolicitudServicioUseCase.class);
        ObtenerDetalleCitaUseCase detalle = mock(ObtenerDetalleCitaUseCase.class);
        Cita cita = new Cita();
        cita.setId(12L);
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setEspecialidadId(2L);
        cita.setDescripcionProblema("fuga");
        when(crear.crear("ana@mail.com", 2L, "fuga", "Calle 1", 4.6, -74.1)).thenReturn(cita);
        when(detalle.detallar(cita)).thenReturn(new DetalleCita(cita, null, null, null, "Plomeria"));

        mvc(new SolicitudServicioController(crear, detalle))
                .perform(post("/api/solicitudes").principal(new UsernamePasswordAuthenticationToken("ana@mail.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidadId\":2,\"descripcionProblema\":\"fuga\",\"direccionServicio\":\"Calle 1\","
                                + "\"latitud\":4.6,\"longitud\":-74.1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.especialidadNombre").value("Plomeria"));
    }

    @Test
    void crearSolicitudSinUbicacionResponde400() throws Exception {
        CrearSolicitudServicioUseCase crear = mock(CrearSolicitudServicioUseCase.class);

        mvc(new SolicitudServicioController(crear, mock(ObtenerDetalleCitaUseCase.class)))
                .perform(post("/api/solicitudes").principal(new UsernamePasswordAuthenticationToken("ana@mail.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidadId\":2,\"descripcionProblema\":\"fuga\",\"direccionServicio\":\"Calle 1\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(crear);
    }
}
