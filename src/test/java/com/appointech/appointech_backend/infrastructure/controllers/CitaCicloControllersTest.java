package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.ports.in.ActualizarEstadoCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarDetalleCitaAdminUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarDiagnosticoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ReprogramarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.AgendamientoExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP del Sprint 4: estado, cancelación, reprogramación, diagnóstico y consulta de citas. */
class CitaCicloControllersTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 3, 10, 0);
    private final Authentication tecnico = new UsernamePasswordAuthenticationToken("tec@mail.com", null);
    private final Authentication cliente = new UsernamePasswordAuthenticationToken("ana@mail.com", null);

    private ActualizarEstadoCitaUseCase actualizarEstado;
    private CancelarCitaTecnicoUseCase cancelarTecnico;
    private RegistrarDiagnosticoUseCase diagnostico;
    private CancelarCitaClienteUseCase cancelarCliente;
    private ReprogramarCitaUseCase reprogramar;
    private ConsultarCitasDelClienteUseCase consultarCliente;
    private ConsultarDetalleCitaAdminUseCase consultarAdmin;
    private ObtenerDetalleCitaUseCase detalleUseCase;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        actualizarEstado = mock(ActualizarEstadoCitaUseCase.class);
        cancelarTecnico = mock(CancelarCitaTecnicoUseCase.class);
        diagnostico = mock(RegistrarDiagnosticoUseCase.class);
        cancelarCliente = mock(CancelarCitaClienteUseCase.class);
        reprogramar = mock(ReprogramarCitaUseCase.class);
        consultarCliente = mock(ConsultarCitasDelClienteUseCase.class);
        consultarAdmin = mock(ConsultarDetalleCitaAdminUseCase.class);
        detalleUseCase = mock(ObtenerDetalleCitaUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new TecnicoGestionCitaController(actualizarEstado, cancelarTecnico, diagnostico, detalleUseCase),
                        new ClienteCitaController(consultarCliente, cancelarCliente, reprogramar, detalleUseCase),
                        new AdminCitaController(consultarAdmin))
                .setControllerAdvice(new GlobalExceptionHandler(), new AgendamientoExceptionHandler(mock(RegistroLogPort.class)))
                .build();
    }

    private Cita cita(Long id, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(id);
        c.setEstado(estado);
        c.setTecnicoId(5L);
        c.setEspecialidadId(2L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Calle 1");
        c.setFechaHoraInicio(INICIO);
        c.setFechaHoraFin(INICIO.plusHours(2));
        return c;
    }

    private DetalleCita detalle(Cita c) {
        return new DetalleCita(c, "Luis", "Perez", "300", "Plomeria");
    }

    // ---------- PATCH /api/tecnico/citas/{id}/estado ----------

    @Test
    void patchEstadoDelegaEnElCasoDeUsoYDevuelveLaCita() throws Exception {
        Cita c = cita(10L, EstadoCita.EN_CAMINO);
        when(actualizarEstado.actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO)).thenReturn(c);
        when(detalleUseCase.detallar(c)).thenReturn(detalle(c));

        mvc.perform(patch("/api/tecnico/citas/10/estado").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CAMINO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"))
                .andExpect(jsonPath("$.tecnicoNombre").value("Luis"))
                .andExpect(jsonPath("$.direccionServicio").value("Calle 1"));
    }

    @Test
    void patchEstadoConTransicionInvalidaResponde409ConMensaje() throws Exception {
        when(actualizarEstado.actualizar(any(), any(), any()))
                .thenThrow(new EstadoCitaInvalidoException("No se puede pasar la cita de CONFIRMADA a FINALIZADA"));

        mvc.perform(patch("/api/tecnico/citas/10/estado").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"FINALIZADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("No se puede pasar la cita de CONFIRMADA a FINALIZADA"));
    }

    @Test
    void patchEstadoDeCitaAjenaOInexistenteResponde404() throws Exception {
        when(actualizarEstado.actualizar(any(), any(), any())).thenThrow(new SolicitudNoEncontradaException(10L));

        mvc.perform(patch("/api/tecnico/citas/10/estado").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CAMINO\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchEstadoSinEstadoOConValorDesconocidoResponde400() throws Exception {
        mvc.perform(patch("/api/tecnico/citas/10/estado").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tecnico/citas/10/estado").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"VOLANDO\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarEstado);
    }

    // ---------- POST /api/tecnico/citas/{id}/cancelar ----------

    @Test
    void tecnicoCancelaConMotivo() throws Exception {
        Cita c = cita(10L, EstadoCita.CANCELADA);
        when(cancelarTecnico.cancelar("tec@mail.com", 10L, "Sin repuesto")).thenReturn(c);
        when(detalleUseCase.detallar(c)).thenReturn(detalle(c));

        mvc.perform(post("/api/tecnico/citas/10/cancelar").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Sin repuesto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void tecnicoCancelarSinMotivoOMotivoLargoResponde400() throws Exception {
        mvc.perform(post("/api/tecnico/citas/10/cancelar").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tecnico/citas/10/cancelar").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cancelarTecnico);
    }

    @Test
    void tecnicoCancelarCitaFinalizadaResponde409() throws Exception {
        when(cancelarTecnico.cancelar(any(), any(), any())).thenThrow(new EstadoCitaInvalidoException("ya finalizada"));

        mvc.perform(post("/api/tecnico/citas/10/cancelar").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void errorDeMotivoDelCasoDeUsoResponde400() throws Exception {
        when(cancelarTecnico.cancelar(any(), any(), any())).thenThrow(new DatosCitaInvalidosException("motivo obligatorio"));

        mvc.perform(post("/api/tecnico/citas/10/cancelar").principal(tecnico).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("motivo obligatorio"));
    }

    // ---------- diagnóstico ----------

    @Test
    void postYPutDelDiagnosticoUsanElMismoCasoDeUso() throws Exception {
        Cita c = cita(10L, EstadoCita.EN_PROCESO);
        c.setDiagnostico("Sifón roto");
        c.setRecomendaciones("Cambiarlo");
        c.setFechaDiagnostico(INICIO);
        when(diagnostico.registrar("tec@mail.com", 10L, "Sifón roto", "Cambiarlo")).thenReturn(c);
        String cuerpo = "{\"diagnostico\":\"Sifón roto\",\"recomendaciones\":\"Cambiarlo\"}";

        mvc.perform(post("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Sifón roto"))
                .andExpect(jsonPath("$.recomendaciones").value("Cambiarlo"))
                .andExpect(jsonPath("$.fecha").exists());
        mvc.perform(put("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Sifón roto"));

        verify(diagnostico, org.mockito.Mockito.times(2)).registrar("tec@mail.com", 10L, "Sifón roto", "Cambiarlo");
    }

    @Test
    void diagnosticoSinTextoOMuyLargoResponde400() throws Exception {
        mvc.perform(post("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"recomendaciones\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diagnostico\":\"" + "d".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"ok\",\"recomendaciones\":\"" + "r".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(diagnostico);
    }

    @Test
    void diagnosticoLimiteExactoSeAcepta() throws Exception {
        Cita c = cita(10L, EstadoCita.EN_PROCESO);
        c.setDiagnostico("d".repeat(2000));
        when(diagnostico.registrar(eq("tec@mail.com"), eq(10L), any(), any())).thenReturn(c);

        mvc.perform(post("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnostico\":\"" + "d".repeat(2000) + "\",\"recomendaciones\":\"" + "r".repeat(1000) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void diagnosticoFueraDeEnProcesoResponde409() throws Exception {
        when(diagnostico.registrar(any(), any(), any(), any()))
                .thenThrow(new EstadoCitaInvalidoException("Solo se puede registrar el diagnóstico cuando la cita está EN_PROCESO"));

        mvc.perform(post("/api/tecnico/citas/10/diagnostico").principal(tecnico)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diagnostico\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- cliente ----------

    @Test
    void clienteListaSusCitasConEstado() throws Exception {
        Cita a = cita(10L, EstadoCita.CONFIRMADA);
        Cita b = cita(11L, EstadoCita.PENDIENTE);
        b.setTecnicoId(null);
        when(consultarCliente.listar("ana@mail.com")).thenReturn(List.of(detalle(a), new DetalleCita(b, null, null, null, "Plomeria")));

        mvc.perform(get("/api/cliente/citas").principal(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].tecnicoNombre").doesNotExist());
    }

    @Test
    void clienteSinCitasRecibeListaVacia() throws Exception {
        when(consultarCliente.listar("ana@mail.com")).thenReturn(List.of());

        mvc.perform(get("/api/cliente/citas").principal(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void clienteVeElDetalleConDiagnosticoEHistorial() throws Exception {
        Cita c = cita(10L, EstadoCita.EN_PROCESO);
        c.setDiagnostico("Sifón roto");
        c.setRecomendaciones("Cambiarlo");
        c.setFechaDiagnostico(INICIO);
        List<HistorialEstadoCita> historial = List.of(
                new HistorialEstadoCita(1L, 10L, EstadoCita.CONFIRMADA, EstadoCita.EN_CAMINO, INICIO, "tec@mail.com", null),
                new HistorialEstadoCita(2L, 10L, EstadoCita.CONFIRMADA, EstadoCita.CONFIRMADA, INICIO, "ana@mail.com", "Reprogramada"));
        when(consultarCliente.detallar("ana@mail.com", 10L)).thenReturn(new DetalleCitaCompleto(detalle(c), historial));

        mvc.perform(get("/api/cliente/citas/10").principal(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.tecnicoNombre").value("Luis"))
                .andExpect(jsonPath("$.especialidadNombre").value("Plomeria"))
                .andExpect(jsonPath("$.direccionServicio").value("Calle 1"))
                .andExpect(jsonPath("$.diagnostico.diagnostico").value("Sifón roto"))
                .andExpect(jsonPath("$.diagnostico.recomendaciones").value("Cambiarlo"))
                .andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.historial[0].estadoAnterior").value("CONFIRMADA"))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("EN_CAMINO"))
                .andExpect(jsonPath("$.historial[0].actor").value("tec@mail.com"))
                .andExpect(jsonPath("$.historial[1].motivo").value("Reprogramada"));
    }

    @Test
    void sinDiagnosticoElCampoViajaNulo() throws Exception {
        Cita c = cita(10L, EstadoCita.CONFIRMADA);
        when(consultarCliente.detallar("ana@mail.com", 10L)).thenReturn(new DetalleCitaCompleto(detalle(c), List.of()));

        mvc.perform(get("/api/cliente/citas/10").principal(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").doesNotExist())
                .andExpect(jsonPath("$.historial.length()").value(0));
    }

    @Test
    void historialConEstadoAnteriorNuloSeSerializaSinEseCampo() throws Exception {
        Cita c = cita(10L, EstadoCita.PENDIENTE);
        List<HistorialEstadoCita> historial = List.of(
                new HistorialEstadoCita(1L, 10L, null, EstadoCita.PENDIENTE, INICIO, "ana@mail.com", null));
        when(consultarCliente.detallar("ana@mail.com", 10L)).thenReturn(new DetalleCitaCompleto(detalle(c), historial));

        mvc.perform(get("/api/cliente/citas/10").principal(cliente))
                .andExpect(jsonPath("$.historial[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    void clienteConCitaAjenaRecibe404() throws Exception {
        when(consultarCliente.detallar("ana@mail.com", 10L)).thenThrow(new SolicitudNoEncontradaException(10L));

        mvc.perform(get("/api/cliente/citas/10").principal(cliente))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteCancelaConYSinCuerpo() throws Exception {
        Cita c = cita(10L, EstadoCita.CANCELADA);
        when(cancelarCliente.cancelar("ana@mail.com", 10L, "Ya no")).thenReturn(c);
        when(cancelarCliente.cancelar("ana@mail.com", 10L, null)).thenReturn(c);
        when(detalleUseCase.detallar(c)).thenReturn(detalle(c));

        mvc.perform(post("/api/cliente/citas/10/cancelar").principal(cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Ya no\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(post("/api/cliente/citas/10/cancelar").principal(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        verify(cancelarCliente).cancelar("ana@mail.com", 10L, "Ya no");
        verify(cancelarCliente).cancelar("ana@mail.com", 10L, null);
    }

    @Test
    void clienteCancelarUnaCitaEnCaminoResponde409() throws Exception {
        when(cancelarCliente.cancelar(any(), any(), any()))
                .thenThrow(new EstadoCitaInvalidoException("Solo se puede cancelar una cita PENDIENTE o CONFIRMADA"));

        mvc.perform(post("/api/cliente/citas/10/cancelar").principal(cliente))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Solo se puede cancelar una cita PENDIENTE o CONFIRMADA"));
    }

    @Test
    void clienteCancelarConMotivoMuyLargoResponde400() throws Exception {
        mvc.perform(post("/api/cliente/citas/10/cancelar").principal(cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cancelarCliente);
    }

    @Test
    void clienteReprogramaConLaNuevaFecha() throws Exception {
        Cita c = cita(10L, EstadoCita.CONFIRMADA);
        when(reprogramar.reprogramar("ana@mail.com", 10L, INICIO)).thenReturn(c);
        when(detalleUseCase.detallar(c)).thenReturn(detalle(c));

        mvc.perform(post("/api/cliente/citas/10/reprogramar").principal(cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inicio\":\"2026-11-03T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.fechaHoraInicio").value("2026-11-03T10:00:00"))
                .andExpect(jsonPath("$.fechaHoraFin").value("2026-11-03T12:00:00"));
    }

    @Test
    void reprogramarSinFranjaDisponibleResponde409() throws Exception {
        when(reprogramar.reprogramar(any(), any(), any()))
                .thenThrow(new FranjaNoDisponibleException("La nueva franja no está disponible"));

        mvc.perform(post("/api/cliente/citas/10/reprogramar").principal(cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inicio\":\"2026-11-03T10:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La nueva franja no está disponible"));
    }

    @Test
    void reprogramarSinFechaResponde400() throws Exception {
        mvc.perform(post("/api/cliente/citas/10/reprogramar").principal(cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reprogramar);
    }

    // ---------- admin ----------

    @Test
    void adminVeElDetalleDeCualquierCita() throws Exception {
        Cita c = cita(10L, EstadoCita.FINALIZADA);
        c.setDiagnostico("ok");
        when(consultarAdmin.detallar(10L)).thenReturn(new DetalleCitaCompleto(detalle(c), List.of()));

        mvc.perform(get("/api/admin/citas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.diagnostico.diagnostico").value("ok"));
    }

    @Test
    void adminConCitaInexistenteRecibe404() throws Exception {
        when(consultarAdmin.detallar(99L)).thenThrow(new SolicitudNoEncontradaException(99L));

        mvc.perform(get("/api/admin/citas/99"))
                .andExpect(status().isNotFound());
    }

    // ---------- roles ----------

    @Test
    void cadaControladorExigeSuRol() {
        assertTrue(TecnicoGestionCitaController.class.getAnnotation(PreAuthorize.class).value().contains("TECNICO"));
        assertTrue(ClienteCitaController.class.getAnnotation(PreAuthorize.class).value().contains("CLIENTE"));
        PreAuthorize admin = AdminCitaController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(admin);
        assertTrue(admin.value().contains("ADMINISTRADOR"));
    }
}
