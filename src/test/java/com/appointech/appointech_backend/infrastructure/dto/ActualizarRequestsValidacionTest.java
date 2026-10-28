package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActualizarRequestsValidacionTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ActualizarClienteRequest cliente(String nombre) {
        return new ActualizarClienteRequest(nombre, null, null, null, null, null, null, null, null);
    }

    private ActualizarTecnicoRequest tecnico(String direccionBase) {
        return new ActualizarTecnicoRequest(null, null, null, null, null, null, direccionBase, null, null, null, null);
    }

    @Test
    void aceptaTextoConAlgunCaracterVisible() {
        assertTrue(validator.validate(cliente("Ana")).isEmpty());
        assertTrue(validator.validate(cliente("  Ana María  ")).isEmpty());
        assertTrue(validator.validate(cliente("\tx")).isEmpty());
        assertTrue(validator.validate(tecnico("Calle 1 # 2-3")).isEmpty());
    }

    @Test
    void aceptaNullPorqueElCampoEsOpcional() {
        assertTrue(validator.validate(cliente(null)).isEmpty());
        assertTrue(validator.validate(tecnico(null)).isEmpty());
    }

    @Test
    void rechazaVacioYSoloEspacios() {
        assertEquals(1, validator.validate(cliente("")).size());
        assertEquals(1, validator.validate(cliente("   ")).size());
        assertEquals(1, validator.validate(cliente(" \t ")).size());
        assertEquals(1, validator.validate(tecnico("   ")).size());
    }

    @Test
    void rechazaTextoConSaltosDeLinea() {
        assertEquals(1, validator.validate(cliente("Ana\nPérez")).size());
        assertEquals(1, validator.validate(cliente("\n")).size());
    }

    @Test
    void entradaLargaDeEspaciosSeEvaluaSinBacktracking() {
        String soloEspacios = " ".repeat(200_000);
        assertTimeoutPreemptivelyRapido(() -> assertEquals(1, validator.validate(cliente(soloEspacios)).size()));
        assertTimeoutPreemptivelyRapido(() -> assertTrue(validator.validate(cliente(soloEspacios + "a")).isEmpty()));
    }

    private void assertTimeoutPreemptivelyRapido(Runnable r) {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), r::run);
    }
}
