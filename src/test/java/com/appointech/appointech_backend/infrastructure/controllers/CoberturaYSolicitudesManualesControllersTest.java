package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudManualNoPermitidaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.TelefonoWhatsappInvalidoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ActualizarCoberturaUseCase;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerCoberturaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.SolicitarAgendamientoManualUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.config.AgendamientoExceptionHandler;
import com.appointech.appointech_backend.infrastructure.config.GlobalExceptionHandler;
import com.appointech.appointech_backend.infrastructure.dto.AsignarSolicitudManualRequest;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.CoberturaRequest;
import com.appointech.appointech_backend.infrastructure.dto.CoberturaResponse;
import com.appointech.appointech_backend.infrastructure.dto.FranjaDisponibleResponse;
import com.appointech.appointech_backend.infrastructure.dto.FranjasConMotivoResponse;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Controladores con seguridad por métodos real (403) y los códigos 200/201/400/404/409 de sus excepciones. */
@SpringJUnitConfig(CoberturaYSolicitudesManualesControllersTest.Config.class)
class CoberturaYSolicitudesManualesControllersTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ObtenerCoberturaUseCase obtenerCobertura() { return mock(ObtenerCoberturaUseCase.class); }
        @Bean ActualizarCoberturaUseCase actualizarCobertura() { return mock(ActualizarCoberturaUseCase.class); }
        @Bean ListarSolicitudesManualesUseCase listarSolicitudes() { return mock(ListarSolicitudesManualesUseCase.class); }
        @Bean AsignarSolicitudManualUseCase asignarSolicitud() { return mock(AsignarSolicitudManualUseCase.class); }
        @Bean ObtenerDetalleCitaUseCase obtenerDetalle() { return mock(ObtenerDetalleCitaUseCase.class); }
        @Bean SolicitarAgendamientoManualUseCase solicitarManual() { return mock(SolicitarAgendamientoManualUseCase.class); }
        @Bean ConfirmarCitaUseCase confirmarCita() { return mock(ConfirmarCitaUseCase.class); }
        @Bean CalcularFranjasDisponiblesUseCase calcularFranjas() { return mock(CalcularFranjasDisponiblesUseCase.class); }
        @Bean FiltrarTecnicosPorEspecialidadUseCase filtrarTecnicos() { return mock(FiltrarTecnicosPorEspecialidadUseCase.class); }

        @Bean AdminCoberturaController adminCoberturaController(ObtenerCoberturaUseCase o, ActualizarCoberturaUseCase a) {
            return new AdminCoberturaController(o, a);
        }
        @Bean AdminSolicitudManualController adminSolicitudManualController(
                ListarSolicitudesManualesUseCase l, AsignarSolicitudManualUseCase a, ObtenerDetalleCitaUseCase d) {
            return new AdminSolicitudManualController(l, a, d);
        }
        @Bean CitaController citaController(ConfirmarCitaUseCase c, ObtenerDetalleCitaUseCase d,
                                            SolicitarAgendamientoManualUseCase s) {
            return new CitaController(c, d, s);
        }
        @Bean DisponibilidadController disponibilidadController(FiltrarTecnicosPorEspecialidadUseCase f,
                                                                CalcularFranjasDisponiblesUseCase c) {
            return new DisponibilidadController(f, c);
        }
    }

    @Autowired AdminCoberturaController cobertura;
    @Autowired AdminSolicitudManualController solicitudesAdmin;
    @Autowired CitaController citaController;
    @Autowired DisponibilidadController disponibilidad;
    @Autowired ObtenerCoberturaUseCase obtenerCobertura;
    @Autowired ActualizarCoberturaUseCase actualizarCobertura;
    @Autowired ListarSolicitudesManualesUseCase listarSolicitudes;
    @Autowired AsignarSolicitudManualUseCase asignarSolicitud;
    @Autowired ObtenerDetalleCitaUseCase obtenerDetalle;
    @Autowired SolicitarAgendamientoManualUseCase solicitarManual;
    @Autowired CalcularFranjasDisponiblesUseCase calcularFranjas;

    private final GlobalExceptionHandler global = new GlobalExceptionHandler();
    private final AgendamientoExceptionHandler agendamiento = new AgendamientoExceptionHandler(mock(RegistroLogPort.class));

    @BeforeEach
    void limpiar() {
        reset(obtenerCobertura, actualizarCobertura, listarSolicitudes, asignarSolicitud, obtenerDetalle,
                solicitarManual, calcularFranjas);
    }

    // ---------- cobertura ----------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminLeeElLimiteConGet200() {
        when(obtenerCobertura.obtener()).thenReturn(new CoberturaConfigurada(15, null));

        ResponseEntity<CoberturaResponse> r = cobertura.obtener();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(15, r.getBody().radioMaximoKm());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminActualizaElLimiteConPut200() {
        when(actualizarCobertura.actualizar(30, null)).thenReturn(new CoberturaConfigurada(30, null));

        ResponseEntity<CoberturaResponse> r = cobertura.actualizar(new CoberturaRequest(30, null));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(30, r.getBody().radioMaximoKm());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void limiteInvalidoSeTraduceA400ConMensajeClaro() {
        when(actualizarCobertura.actualizar(500, null)).thenThrow(
                new RadioCoberturaInvalidoException("El radio máximo de cobertura debe ser mayor que 0 y como máximo 100 km"));

        RadioCoberturaInvalidoException ex = assertThrows(RadioCoberturaInvalidoException.class,
                () -> cobertura.actualizar(new CoberturaRequest(500, null)));

        var problema = global.manejarDatosInvalidos(ex, peticion("/api/admin/configuracion/cobertura"));
        assertEquals(400, problema.getStatus());
        assertEquals("El radio máximo de cobertura debe ser mayor que 0 y como máximo 100 km", problema.getDetail());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unClienteNoPuedeLeerNiCambiarElLimite403() {
        assertThrows(AccessDeniedException.class, () -> cobertura.obtener());
        assertThrows(AccessDeniedException.class, () -> cobertura.actualizar(new CoberturaRequest(20, null)));
        verifyNoInteractions(actualizarCobertura);
    }

    // ---------- solicitud manual del cliente ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteCreaSolicitudManualCon201() {
        Cita solicitud = pendienteManual();
        when(solicitarManual.solicitar(any(), any(), any(), any(), any(), any(), any())).thenReturn(solicitud);
        when(obtenerDetalle.detallar(solicitud)).thenReturn(new DetalleCita(solicitud, null, null, null, "Plomería"));

        ResponseEntity<CitaResponse> r = citaController.solicitudManual(
                new SolicitudManualRequest(5L, "Fuga", "Vereda", 4.5, -74.1, "3109998877"), auth());

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("PENDIENTE", r.getBody().estado());
        assertNull(r.getBody().tecnicoId());
        assertNull(r.getBody().fechaHoraInicio());
        assertNull(r.getBody().tecnicoNombre());
        assertEquals("Plomería", r.getBody().especialidadNombre());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void solicitudManualDentroDeCoberturaSeTraduceA409() {
        when(solicitarManual.solicitar(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new SolicitudManualNoPermitidaException("La dirección está dentro de la cobertura"));

        SolicitudManualNoPermitidaException ex = assertThrows(SolicitudManualNoPermitidaException.class,
                () -> citaController.solicitudManual(new SolicitudManualRequest(5L, "Fuga", "Calle", 4.5, -74.1, null), auth()));

        assertEquals(409, global.manejarSolicitudManualNoPermitida(ex, peticion("/api/citas/solicitud-manual")).getStatus());
    }

    @Test
    @WithMockUser(roles = "TECNICO")
    void soloElClientePuedeCrearSolicitudManual403() {
        assertThrows(AccessDeniedException.class, () -> citaController.solicitudManual(
                new SolicitudManualRequest(5L, "Fuga", "Calle", 4.5, -74.1, null), auth()));
        verifyNoInteractions(solicitarManual);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void elAdminNoCreaSolicitudesManualesPorElEndpointDelCliente403() {
        assertThrows(AccessDeniedException.class, () -> citaController.solicitudManual(
                new SolicitudManualRequest(5L, "Fuga", "Calle", 4.5, -74.1, null), auth()));
    }

    // ---------- admin: listar y asignar ----------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminListaLasSolicitudesManualesCon200() {
        when(listarSolicitudes.listar()).thenReturn(List.of(new SolicitudManualDetalle(
                70L, 2L, "Ana", "Ruiz", "3109998877", "Vereda", "Fuga", 5L, "Plomería", 4.5, -74.1, INICIO)));

        ResponseEntity<List<SolicitudManualAdminResponse>> r = solicitudesAdmin.listar();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        SolicitudManualAdminResponse s = r.getBody().get(0);
        assertEquals("Ana", s.clienteNombre());
        assertEquals("3109998877", s.telefono());
        assertEquals("Vereda", s.direccionServicio());
        assertEquals("Fuga", s.descripcionProblema());
        assertEquals("Plomería", s.especialidadNombre());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminAsignaConPatch200YLaRespuestaTraeElTecnico() {
        Cita confirmada = pendienteManual();
        confirmada.setEstado(EstadoCita.CONFIRMADA);
        confirmada.setTecnicoId(3L);
        confirmada.setFechaHoraInicio(INICIO);
        confirmada.setFechaHoraFin(INICIO.plusHours(2));
        when(asignarSolicitud.asignar(eq("admin"), eq(70L), eq(3L), eq(INICIO), eq(INICIO.plusHours(2)))).thenReturn(confirmada);
        when(obtenerDetalle.detallar(confirmada)).thenReturn(new DetalleCita(confirmada, "Luis", "Mora", "311", "Plomería"));

        ResponseEntity<CitaResponse> r = solicitudesAdmin.asignar(70L,
                new AsignarSolicitudManualRequest(3L, INICIO, INICIO.plusHours(2)), auth("admin"));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("CONFIRMADA", r.getBody().estado());
        assertEquals(3L, r.getBody().tecnicoId());
        assertEquals("Luis", r.getBody().tecnicoNombre());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void erroresDeAsignacionSeTraducenA404409y400() {
        var peticion = peticion("/api/admin/solicitudes-manuales/70/asignar");
        var req = new AsignarSolicitudManualRequest(3L, INICIO, INICIO.plusHours(2));

        when(asignarSolicitud.asignar(any(), eq(1L), any(), any(), any())).thenThrow(new SolicitudNoEncontradaException(1L));
        when(asignarSolicitud.asignar(any(), eq(2L), any(), any(), any())).thenThrow(new TecnicoNoEncontradoException(3L));
        when(asignarSolicitud.asignar(any(), eq(3L), any(), any(), any())).thenThrow(new FranjaNoDisponibleException("choque"));
        when(asignarSolicitud.asignar(any(), eq(4L), any(), any(), any())).thenThrow(new FranjaInvalidaException("fin"));

        var e404 = assertThrows(SolicitudNoEncontradaException.class, () -> solicitudesAdmin.asignar(1L, req, auth()));
        var e404t = assertThrows(TecnicoNoEncontradoException.class, () -> solicitudesAdmin.asignar(2L, req, auth()));
        var e409 = assertThrows(FranjaNoDisponibleException.class, () -> solicitudesAdmin.asignar(3L, req, auth()));
        var e400 = assertThrows(FranjaInvalidaException.class, () -> solicitudesAdmin.asignar(4L, req, auth()));

        assertEquals(HttpStatus.NOT_FOUND, agendamiento.manejarSolicitudNoEncontrada(e404).getStatusCode());
        assertEquals(404, global.manejarTecnicoNoEncontrado(e404t, peticion).getStatus());
        assertEquals(HttpStatus.CONFLICT, agendamiento.manejarFranjaNoDisponible(e409).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, agendamiento.manejarFranjaInvalida(e400).getStatusCode());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unClienteNoPuedeVerNiAsignarSolicitudesManuales403() {
        assertThrows(AccessDeniedException.class, () -> solicitudesAdmin.listar());
        assertThrows(AccessDeniedException.class, () -> solicitudesAdmin.asignar(70L,
                new AsignarSolicitudManualRequest(3L, INICIO, INICIO.plusHours(2)), auth()));
        verifyNoInteractions(asignarSolicitud);
    }

    @Test
    @WithMockUser(roles = "TECNICO")
    void unTecnicoNoPuedeVerSolicitudesManuales403() {
        assertThrows(AccessDeniedException.class, () -> solicitudesAdmin.listar());
    }

    // ---------- franjas con motivo ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    void franjasConMotivoInformaElMotivoCuandoNoHayFranjas() {
        when(calcularFranjas.calcularConMotivo(any(), any(), any(), any()))
                .thenReturn(new ResultadoFranjas(List.of(), MotivoSinFranjas.FUERA_DE_COBERTURA));

        ResponseEntity<FranjasConMotivoResponse> r =
                disponibilidad.franjasConMotivo(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("FUERA_DE_COBERTURA", r.getBody().motivoSinFranjas());
        assertEquals(List.of(), r.getBody().franjas());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void franjasConMotivoDejaElMotivoNuloSiHayFranjas() {
        when(calcularFranjas.calcularConMotivo(any(), any(), any(), any())).thenReturn(new ResultadoFranjas(
                List.of(new FranjaDisponible(3L, INICIO, INICIO.plusHours(2), 3.0, "Ana", "Ruiz")), null));

        FranjasConMotivoResponse r = disponibilidad.franjasConMotivo(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1).getBody();

        assertNull(r.motivoSinFranjas());
        assertEquals("Ana", r.franjas().get(0).tecnicoNombre());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void elEndpointActualDeFranjasSigueDevolviendoUnArregloYPonePorCabeceraElMotivo() {
        when(calcularFranjas.calcularConMotivo(any(), any(), any(), any()))
                .thenReturn(new ResultadoFranjas(List.of(), MotivoSinFranjas.SIN_TECNICOS_ESPECIALIDAD));

        ResponseEntity<List<FranjaDisponibleResponse>> r =
                disponibilidad.franjas(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1);

        assertEquals(List.of(), r.getBody());
        assertEquals("SIN_TECNICOS_ESPECIALIDAD", r.getHeaders().getFirst("X-Motivo-Sin-Franjas"));
    }

    // ---------- teléfono de WhatsApp del administrador ----------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void getYPutDeCoberturaIncluyenElTelefonoDeWhatsapp() {
        when(obtenerCobertura.obtener()).thenReturn(new CoberturaConfigurada(15, "+57 300 123 4567"));
        when(actualizarCobertura.actualizar(20, "3001234567")).thenReturn(new CoberturaConfigurada(20, "3001234567"));

        assertEquals("+57 300 123 4567", cobertura.obtener().getBody().telefonoWhatsappAdmin());
        CoberturaResponse r = cobertura.actualizar(new CoberturaRequest(20, "3001234567")).getBody();
        assertEquals(20, r.radioMaximoKm());
        assertEquals("3001234567", r.telefonoWhatsappAdmin());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void telefonoDeWhatsappInvalidoSeTraduceA400ConMensajeClaro() {
        when(actualizarCobertura.actualizar(15, "123")).thenThrow(new TelefonoWhatsappInvalidoException(
                "El teléfono de WhatsApp debe tener entre 10 y 15 dígitos"));

        TelefonoWhatsappInvalidoException ex = assertThrows(TelefonoWhatsappInvalidoException.class,
                () -> cobertura.actualizar(new CoberturaRequest(15, "123")));

        var problema = global.manejarDatosInvalidos(ex, peticion("/api/admin/configuracion/cobertura"));
        assertEquals(400, problema.getStatus());
        assertEquals("El teléfono de WhatsApp debe tener entre 10 y 15 dígitos", problema.getDetail());
    }

    @Test
    @WithMockUser(roles = "TECNICO")
    void unTecnicoNoPuedeCambiarElTelefonoDeWhatsapp403() {
        assertThrows(AccessDeniedException.class, () -> cobertura.actualizar(new CoberturaRequest(15, "3001234567")));
        verifyNoInteractions(actualizarCobertura);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void fueraDeCoberturaLaRespuestaDeFranjasTraeElTelefonoDeContactoDelAdmin() {
        when(calcularFranjas.calcularConMotivo(any(), any(), any(), any())).thenReturn(
                new ResultadoFranjas(List.of(), MotivoSinFranjas.FUERA_DE_COBERTURA, "+57 300 123 4567"));

        FranjasConMotivoResponse r = disponibilidad.franjasConMotivo(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1).getBody();
        ResponseEntity<List<FranjaDisponibleResponse>> arreglo =
                disponibilidad.franjas(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1);

        assertEquals("FUERA_DE_COBERTURA", r.motivoSinFranjas());
        assertEquals("+57 300 123 4567", r.telefonoContactoAdmin());
        assertEquals("+57 300 123 4567", arreglo.getHeaders().getFirst("X-Telefono-Contacto-Admin"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void sinTelefonoConfiguradoElCampoYLaCabeceraNoVienen() {
        when(calcularFranjas.calcularConMotivo(any(), any(), any(), any()))
                .thenReturn(new ResultadoFranjas(List.of(), MotivoSinFranjas.FUERA_DE_COBERTURA, null));

        FranjasConMotivoResponse r = disponibilidad.franjasConMotivo(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1).getBody();
        var arreglo = disponibilidad.franjas(5L, LocalDate.of(2030, 5, 6), 4.5, -74.1);

        assertNull(r.telefonoContactoAdmin());
        assertNull(arreglo.getHeaders().getFirst("X-Telefono-Contacto-Admin"));
    }

    // ---------- helpers ----------

    private static Authentication auth() {
        return auth("cliente@x.com");
    }

    private static Authentication auth(String nombre) {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn(nombre);
        return a;
    }

    private static HttpServletRequest peticion(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private static Cita pendienteManual() {
        Cita c = new Cita();
        c.setId(70L);
        c.setClienteId(2L);
        c.setEspecialidadId(5L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Vereda");
        c.setEstado(EstadoCita.PENDIENTE);
        c.setAgendamientoManual(true);
        return c;
    }
}
