package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ConsultarAlertasAdminUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AlertaAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.AlertasAdminResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Solo el administrador ve las alertas; la lista se arma desde las solicitudes marcadas. */
@SpringJUnitConfig(AdminAlertaControllerTest.Config.class)
class AdminAlertaControllerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 5, 6, 9, 0);

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ConsultarAlertasAdminUseCase consultarAlertas() { return mock(ConsultarAlertasAdminUseCase.class); }

        @Bean AdminAlertaController adminAlertaController(ConsultarAlertasAdminUseCase c) {
            return new AdminAlertaController(c);
        }
    }

    @Autowired AdminAlertaController controlador;
    @Autowired ConsultarAlertasAdminUseCase consultarAlertas;

    @BeforeEach
    void limpiar() {
        reset(consultarAlertas);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminVeLasAlertasConSuContador() {
        when(consultarAlertas.sinTecnicosParaLaEspecialidad()).thenReturn(List.of(
                new SolicitudManualDetalle(70L, 2L, "Ana", "Ruiz", "3001112233", "Calle 1", "Fuga", 5L, "Plomería",
                        4.5, -74.1, FECHA, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD)));

        ResponseEntity<AlertasAdminResponse> r = controlador.listar();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        AlertasAdminResponse cuerpo = r.getBody();
        assertNotNull(cuerpo);
        assertEquals(1, cuerpo.total());
        AlertaAdminResponse a = cuerpo.alertas().get(0);
        assertEquals("SIN_TECNICOS_ESPECIALIDAD", a.tipo());
        assertEquals("No hay técnicos con la especialidad Plomería para la solicitud 70", a.mensaje());
        assertEquals(70L, a.solicitudId());
        assertEquals(2L, a.clienteId());
        assertEquals("Ana", a.clienteNombre());
        assertEquals("Ruiz", a.clienteApellido());
        assertEquals("3001112233", a.telefono());
        assertEquals(5L, a.especialidadId());
        assertEquals("Plomería", a.especialidadNombre());
        assertEquals("Calle 1", a.direccionServicio());
        assertEquals(FECHA, a.creadaEn());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void sinAlertasDevuelveTotalCeroYListaVacia() {
        when(consultarAlertas.sinTecnicosParaLaEspecialidad()).thenReturn(List.of());

        AlertasAdminResponse cuerpo = controlador.listar().getBody();

        assertNotNull(cuerpo);
        assertEquals(0, cuerpo.total());
        assertTrue(cuerpo.alertas().isEmpty());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unClienteNoPuedeVerLasAlertas403() {
        assertThrows(AccessDeniedException.class, () -> controlador.listar());
        verifyNoInteractions(consultarAlertas);
    }

    @Test
    @WithMockUser(roles = "TECNICO")
    void unTecnicoNoPuedeVerLasAlertas403() {
        assertThrows(AccessDeniedException.class, () -> controlador.listar());
        verifyNoInteractions(consultarAlertas);
    }
}
