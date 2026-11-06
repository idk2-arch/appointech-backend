package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.TecnicoDisponible;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarTecnicosDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.AgendamientoExceptionHandler;
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

/** GET /api/admin/solicitudes-manuales con la seguridad real (JWT, roles, CORS) y el manejador de errores real. */
@WebMvcTest(value = AdminSolicitudManualController.class, properties = "jwt.secret=secreto-de-pruebas-de-al-menos-32-caracteres!!")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, AgendamientoExceptionHandler.class})
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
    private ConsultarTecnicosDisponiblesUseCase consultarTecnicos;
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

    // ---------- GET /{id}/tecnicos-disponibles (RF15) ----------

    private static final String RUTA_TECNICOS = RUTA + "/15/tecnicos-disponibles";
    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);

    @Test
    void administradorRecibeLosTecnicosConDistanciaRedondeadaYFotoUrl() throws Exception {
        when(consultarTecnicos.consultar(15L, INICIO, INICIO.plusHours(2))).thenReturn(List.of(
                new TecnicoDisponible(3L, "Ana", "Ruiz", "311", "11111111-2222-3333-4444-555555555555", 3.14159),
                new TecnicoDisponible(4L, "Luis", "Mora", null, null, null)));

        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tecnicoId").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].apellido").value("Ruiz"))
                .andExpect(jsonPath("$[0].telefono").value("311"))
                .andExpect(jsonPath("$[0].fotoUrl").value("/api/fotos/11111111-2222-3333-4444-555555555555"))
                .andExpect(jsonPath("$[0].distanciaKm").value(3.14))
                .andExpect(jsonPath("$[1].tecnicoId").value(4))
                .andExpect(jsonPath("$[1].distanciaKm").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].fotoUrl").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void sinTecnicosDisponiblesDevuelve200ConArregloVacio() throws Exception {
        when(consultarTecnicos.consultar(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void inicioOFinFaltantesOMalEscritosResponden400ConMensaje() throws Exception {
        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR")).param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("inicio es obligatorio (formato 2030-05-06T09:00:00)"));
        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "mañana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fin debe tener el formato 2030-05-06T09:00:00"));
        verifyNoInteractions(consultarTecnicos);
    }

    @Test
    void finNoPosteriorAlInicioResponde400() throws Exception {
        when(consultarTecnicos.consultar(any(), any(), any()))
                .thenThrow(new FranjaInvalidaException("La hora de fin debe ser posterior a la de inicio"));

        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T11:00:00").param("fin", "2030-05-06T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La hora de fin debe ser posterior a la de inicio"));
    }

    @Test
    void solicitudInexistenteResponde404YYaAsignada409() throws Exception {
        when(consultarTecnicos.consultar(eq(99L), any(), any())).thenThrow(new SolicitudNoEncontradaException(99L));
        when(consultarTecnicos.consultar(eq(15L), any(), any()))
                .thenThrow(new FranjaNoDisponibleException("La solicitud ya no está pendiente"));

        mvc.perform(get(RUTA + "/99/tecnicos-disponibles").header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isNotFound());
        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("ADMINISTRADOR"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isConflict());
    }

    @Test
    void tecnicosDisponiblesSoloParaAdministrador() throws Exception {
        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("CLIENTE"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA_TECNICOS).header("Authorization", token("TECNICO"))
                        .param("inicio", "2030-05-06T09:00:00").param("fin", "2030-05-06T11:00:00"))
                .andExpect(status().isForbidden());
        mvc.perform(get(RUTA_TECNICOS)).andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarTecnicos);
    }

    // ---------- PATCH /{id}/asignar: mismo contrato + trazabilidad ----------

    @Test
    void asignarDevuelveElContratoDeSiempreConLaTrazabilidadAlFinal() throws Exception {
        Cita cita = new Cita();
        cita.setId(15L);
        cita.setTecnicoId(3L);
        cita.setEspecialidadId(9L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setAgendamientoManual(true);
        cita.setAsignacionManual(true);
        cita.setAsignadaPorId(1L);
        cita.setAsignadaEn(LocalDateTime.of(2030, 5, 2, 15, 0));
        when(asignar.asignar(any(), eq(15L), eq(3L), any(), any())).thenReturn(cita);
        when(obtenerDetalle.detallar(cita)).thenReturn(
                new DetalleCita(cita, "Ana", "Ruiz", "311", "Aires acondicionados", null, "Root Admin"));

        mvc.perform(patch(RUTA + "/15/asignar").header("Authorization", token("ADMINISTRADOR"))
                        .contentType("application/json")
                        .content("{\"tecnicoId\":3,\"fechaHoraInicio\":\"2030-05-06T09:00:00\",\"fechaHoraFin\":\"2030-05-06T11:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.tecnicoNombre").value("Ana"))
                .andExpect(jsonPath("$.asignadaManualmente").value(true))
                .andExpect(jsonPath("$.asignacionManual.asignadaPorId").value(1))
                .andExpect(jsonPath("$.asignacionManual.asignadaPorNombre").value("Root Admin"))
                .andExpect(jsonPath("$.asignacionManual.asignadaEn").value("2030-05-02T15:00:00"));
    }
}
