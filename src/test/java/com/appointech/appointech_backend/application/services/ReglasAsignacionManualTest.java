package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReglasAsignacionManualTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);
    private static final LocalDateTime FIN = INICIO.plusHours(2);

    private CitaRepositoryPort citaRepo;
    private BloqueIndisponibilidadRepositoryPort bloquesRepo;
    private ReglasAsignacionManual reglas;

    @BeforeEach
    void preparar() {
        citaRepo = mock(CitaRepositoryPort.class);
        bloquesRepo = mock(BloqueIndisponibilidadRepositoryPort.class);
        reglas = new ReglasAsignacionManual(citaRepo, bloquesRepo);
    }

    private static Cita cita(long id, LocalDateTime inicio, LocalDateTime fin) {
        Cita c = new Cita();
        c.setId(id);
        c.setFechaHoraInicio(inicio);
        c.setFechaHoraFin(fin);
        return c;
    }

    private static BloqueIndisponibilidad bloque(LocalDateTime inicio, LocalDateTime fin) {
        BloqueIndisponibilidad b = mock(BloqueIndisponibilidad.class);
        when(b.getInicio()).thenReturn(inicio);
        when(b.getFin()).thenReturn(fin);
        return b;
    }

    @Test
    void sinCitasNiBloqueosElTecnicoEstaLibre() {
        assertFalse(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));
    }

    @Test
    void citasQueTerminanEnElInicioOEmpiezanEnElFinNoSeCruzan() {
        when(citaRepo.buscarActivasDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(
                cita(1L, INICIO.minusHours(2), INICIO), cita(2L, FIN, FIN.plusHours(1))));

        assertFalse(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));
    }

    @Test
    void unaCitaQueSeSuperponeOcupaAlTecnicoPeroNoLaPropiaSolicitud() {
        when(citaRepo.buscarActivasDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(cita(1L, INICIO.plusHours(1), FIN.plusHours(1))));
        assertTrue(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));

        when(citaRepo.buscarActivasDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(cita(15L, INICIO, FIN)));
        assertFalse(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));
    }

    @Test
    void bloqueosQueSoloColindanNoOcupanYLosQueSeSuperponenSi() {
        BloqueIndisponibilidad antes = bloque(INICIO.minusHours(1), INICIO);
        BloqueIndisponibilidad despues = bloque(FIN, FIN.plusHours(1));
        BloqueIndisponibilidad superpuesto = bloque(INICIO.plusMinutes(30), FIN.plusHours(3));

        when(bloquesRepo.buscarDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(antes, despues));
        assertFalse(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));

        when(bloquesRepo.buscarDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(superpuesto));
        assertTrue(reglas.tecnicoOcupado(3L, 15L, INICIO, FIN));
    }
}
