package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/admin/solicitudes-manuales con la seguridad real (JWT, roles, CORS) y el manejador de errores real. */
@WebMvcTest(value = AdminSolicitudManualController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AdminSolicitudManualListadoHttpTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-caracteres!!";
    private static final String RUTA = "/api/admin/solicitudes-manuales";
    private static final LocalDateTime CREADA = LocalDateTime.of(2030, 5, 1, 8, 0);

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mvc;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HabilitaSeguridadWeb {
    }

    @MockitoBean
    private ListarSolicitudesManualesUseCase listar;
    @MockitoBean
    private AsignarSolicitudManualUseCase asignar;
    @MockitoBean
    private ObtenerDetalleCitaUseCase obtenerDetalle;
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

    private ListadoConTotal<SolicitudManualDetalle> unaSolicitud(long total) {
        return new ListadoConTotal<>(List.of(new SolicitudManualDetalle(15L, 2L, "Ana", "Ruiz", "3001112233",
                "Calle 1", "No enfría", 9L, "Aires acondicionados", 4.6, -74.0, CREADA,
                MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, EstadoCita.PENDIENTE)), total);
    }

    @Test
    void sinParametrosDevuelveUnArregloConElContratoActualMasEstadoYElTotalEnElEncabezado() throws Exception {
        when(listar.listar(any())).thenReturn(unaSolicitud(1));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].clienteNombre").value("Ana"))
                .andExpect(jsonPath("$[0].telefono").value("3001112233"))
                .andExpect(jsonPath("$[0].especialidadNombre").value("Aires acondicionados"))
                .andExpect(jsonPath("$[0].motivo").value("SIN_TECNICOS_ESPECIALIDAD"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));

        ArgumentCaptor<FiltroSolicitudesManuales> captor = ArgumentCaptor.forClass(FiltroSolicitudesManuales.class);
        verify(listar).listar(captor.capture());
        assertEquals(FiltroSolicitudesManuales.sinFiltros(), captor.getValue());
    }

    @Test
    void pasaFiltrosYPaginacionYElTotalEsElDeTodoElFiltro() throws Exception {
        when(listar.listar(any())).thenReturn(unaSolicitud(42));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR"))
                        .param("motivo", "SIN_TECNICOS_ESPECIALIDAD").param("especialidadId", "9")
                        .param("estado", "CONFIRMADA").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "42"));

        ArgumentCaptor<FiltroSolicitudesManuales> captor = ArgumentCaptor.forClass(FiltroSolicitudesManuales.class);
        verify(listar).listar(captor.capture());
        assertEquals(new FiltroSolicitudesManuales(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, 9L,
                EstadoCita.CONFIRMADA, 1, 10), captor.getValue());
    }

    @Test
    void elEncabezadoTotalQuedaExpuestoALosNavegadoresDelFrontend() throws Exception {
        when(listar.listar(any())).thenReturn(unaSolicitud(1));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Total-Count"));
    }

    @Test
    void motivoOEstadoDesconocidosRespondenElMismoProblemDetail400() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("motivo", "OTRO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para motivo: OTRO"))
                .andExpect(jsonPath("$.instance").value(RUTA));
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("estado", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para estado: XYZ"));
        verifyNoInteractions(listar);
    }

    @Test
    void paginaNegativaOTamanioFueraDeRangoRespondenProblemDetail400() throws Exception {
        when(listar.listar(any())).thenThrow(new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y 100"));

        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("page", "0").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El tamaño debe estar entre 1 y 100"));
    }

    @Test
    void unaPaginaQueNoEsNumeroResponde400() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("ADMINISTRADOR")).param("page", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listar);
    }

    @Test
    void tecnicoYClienteReciben403() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", token("TECNICO"))).andExpect(status().isForbidden());
        mvc.perform(get(RUTA).header("Authorization", token("CLIENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(listar);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(listar);
    }
}
