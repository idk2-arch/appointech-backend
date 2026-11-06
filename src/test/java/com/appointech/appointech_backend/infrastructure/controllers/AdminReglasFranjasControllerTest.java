package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ReglasFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoActualizacionReglas;
import com.appointech.appointech_backend.domain.ports.in.ActualizarReglasFranjasUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerReglasFranjasUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ReglasFranjasRequest;
import com.appointech.appointech_backend.infrastructure.dto.ReglasFranjasResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Seguridad por métodos real (solo ADMINISTRADOR), respuesta con advertencia y validación del cuerpo. */
@SpringJUnitConfig(AdminReglasFranjasControllerTest.Config.class)
class AdminReglasFranjasControllerTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime SEIS_PM = LocalTime.of(18, 0);

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ObtenerReglasFranjasUseCase obtenerReglas() { return mock(ObtenerReglasFranjasUseCase.class); }
        @Bean ActualizarReglasFranjasUseCase actualizarReglas() { return mock(ActualizarReglasFranjasUseCase.class); }

        @Bean AdminReglasFranjasController controlador(ObtenerReglasFranjasUseCase o, ActualizarReglasFranjasUseCase a) {
            return new AdminReglasFranjasController(o, a);
        }
    }

    @Autowired AdminReglasFranjasController controlador;
    @Autowired ObtenerReglasFranjasUseCase obtener;
    @Autowired ActualizarReglasFranjasUseCase actualizar;

    @BeforeEach
    void limpiar() {
        reset(obtener, actualizar);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminLeeLasReglasConGet200SinAdvertencia() {
        when(obtener.obtener()).thenReturn(new ReglasFranjas(15, 120, OCHO, SEIS_PM));

        ResponseEntity<ReglasFranjasResponse> r = controlador.obtener();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        ReglasFranjasResponse cuerpo = r.getBody();
        assertNotNull(cuerpo);
        assertEquals(120, cuerpo.duracionBloqueMinutos());
        assertEquals(OCHO, cuerpo.horaInicioJornada());
        assertFalse(cuerpo.franjasOfrecidasInvalidadas());
        assertNull(cuerpo.advertencia());
    }

    @Test
    @WithMockUser(username = "admin@appointech.com", roles = "ADMINISTRADOR")
    void adminActualizaConPut200YRecibeAdvertenciaSiSeInvalidanFranjas() {
        when(actualizar.actualizar("admin@appointech.com", 30, 60, OCHO, SEIS_PM)).thenReturn(
                new ResultadoActualizacionReglas(new ReglasFranjas(30, 60, OCHO, SEIS_PM), true));

        ResponseEntity<ReglasFranjasResponse> r = controlador.actualizar(
                new ReglasFranjasRequest(30, 60, OCHO, SEIS_PM), autenticacion());

        assertEquals(HttpStatus.OK, r.getStatusCode());
        ReglasFranjasResponse cuerpo = r.getBody();
        assertNotNull(cuerpo);
        assertEquals(30, cuerpo.radioMaximoKm());
        assertEquals(60, cuerpo.duracionBloqueMinutos());
        assertTrue(cuerpo.franjasOfrecidasInvalidadas());
        assertEquals(AdminReglasFranjasController.ADVERTENCIA_FRANJAS_INVALIDADAS, cuerpo.advertencia());
    }

    @Test
    @WithMockUser(username = "admin@appointech.com", roles = "ADMINISTRADOR")
    void sinCambioDeDuracionNiJornadaNoHayAdvertencia() {
        when(actualizar.actualizar("admin@appointech.com", 40, 120, OCHO, SEIS_PM)).thenReturn(
                new ResultadoActualizacionReglas(new ReglasFranjas(40, 120, OCHO, SEIS_PM), false));

        ReglasFranjasResponse cuerpo = controlador.actualizar(
                new ReglasFranjasRequest(40, 120, OCHO, SEIS_PM), autenticacion()).getBody();

        assertNotNull(cuerpo);
        assertFalse(cuerpo.franjasOfrecidasInvalidadas());
        assertNull(cuerpo.advertencia());
    }

    @Test
    @WithMockUser(roles = {"CLIENTE"})
    void unClienteNoPuedeLeerNiCambiarLasReglas403() {
        assertThrows(AccessDeniedException.class, () -> controlador.obtener());
        assertThrows(AccessDeniedException.class, () -> controlador.actualizar(
                new ReglasFranjasRequest(30, 60, OCHO, SEIS_PM), autenticacion()));
        verifyNoInteractions(actualizar);
    }

    @Test
    @WithMockUser(roles = {"TECNICO"})
    void unTecnicoTampocoPuedeCambiarLasReglas403() {
        assertThrows(AccessDeniedException.class, () -> controlador.actualizar(
                new ReglasFranjasRequest(30, 60, OCHO, SEIS_PM), autenticacion()));
        verifyNoInteractions(actualizar);
    }

    @Test
    void elCuerpoExigeTodosLosCampos() {
        try (var fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator validador = fabrica.getValidator();

            assertTrue(validador.validate(new ReglasFranjasRequest(30, 60, OCHO, SEIS_PM)).isEmpty());
            assertEquals(4, validador.validate(new ReglasFranjasRequest(null, null, null, null)).size());
        }
    }

    private static Authentication autenticacion() {
        Authentication autenticacion = mock(Authentication.class);
        when(autenticacion.getName()).thenReturn("admin@appointech.com");
        return autenticacion;
    }
}
