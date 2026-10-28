package com.appointech.appointech_backend.domain.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracionRutaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 3, 10, 9, 0);

    @Test
    void llegarAntesODeLaHoraNoGeneraRetraso() {
        ConfiguracionRuta config = new ConfiguracionRuta(0);
        assertEquals(0, config.retrasoEstimadoMinutos(INICIO.minusMinutes(5), INICIO));
        assertEquals(0, config.retrasoEstimadoMinutos(INICIO, INICIO));
    }

    @Test
    void elRetrasoSeRedondeaHaciaArribaYIncluyeElMargen() {
        ConfiguracionRuta config = new ConfiguracionRuta(5);
        assertEquals(6, config.retrasoEstimadoMinutos(INICIO.plusMinutes(1), INICIO));
        assertEquals(1, new ConfiguracionRuta(0).retrasoEstimadoMinutos(INICIO.plusSeconds(1), INICIO));
    }

    @Test
    void elCalculoAtravesandoMedianocheEsConsistente() {
        LocalDateTime inicio = LocalDateTime.of(2030, 3, 10, 23, 50);
        assertEquals(20, new ConfiguracionRuta(0).retrasoEstimadoMinutos(LocalDateTime.of(2030, 3, 11, 0, 10), inicio));
    }
}
