package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaRepositoryAdapterTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 5, 4, 9, 0);

    @Mock
    private CitaJpaRepository jpa;
    @InjectMocks
    private CitaRepositoryAdapter adapter;

    private CitaEntity entity(Long id, EstadoCitaEntity estado) {
        CitaEntity e = new CitaEntity();
        e.setId(id);
        e.setClienteId(1L);
        e.setTecnicoId(2L);
        e.setEspecialidadId(3L);
        e.setDescripcionProblema("fuga");
        e.setDireccionServicio("Calle 5");
        e.setLatitud(4.1);
        e.setLongitud(-74.2);
        e.setFechaHoraInicio(INICIO);
        e.setFechaHoraFin(INICIO.plusHours(1));
        e.setEstado(estado);
        e.setDiagnostico("ok");
        e.setCreadaEn(INICIO.minusDays(1));
        e.setActualizadaEn(INICIO.minusHours(1));
        e.setAgendamientoManual(true);
        e.setTelefonoContacto("3111111111");
        return e;
    }

    @Test
    void guardarConvierteTodosLosCamposYEstado() {
        Cita c = new Cita();
        c.setClienteId(1L);
        c.setTecnicoId(2L);
        c.setEspecialidadId(3L);
        c.setDescripcionProblema("fuga");
        c.setEstado(EstadoCita.EN_CAMINO);
        c.setAgendamientoManual(true);
        c.setTelefonoContacto("3111111111");
        c.setFechaHoraInicio(INICIO);
        when(jpa.save(any(CitaEntity.class))).thenAnswer(inv -> {
            CitaEntity e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });

        Cita guardada = adapter.guardar(c);

        ArgumentCaptor<CitaEntity> captor = ArgumentCaptor.forClass(CitaEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(EstadoCitaEntity.EN_CAMINO, captor.getValue().getEstado());
        assertEquals(50L, guardada.getId());
        assertEquals(EstadoCita.EN_CAMINO, guardada.getEstado());
        assertTrue(guardada.isAgendamientoManual());
        assertEquals("3111111111", guardada.getTelefonoContacto());
        assertEquals(INICIO, guardada.getFechaHoraInicio());
    }

    @Test
    void buscarPorIdMapeaYRespetaAusencia() {
        when(jpa.findById(1L)).thenReturn(Optional.of(entity(1L, EstadoCitaEntity.FINALIZADA)));
        when(jpa.findById(2L)).thenReturn(Optional.empty());

        Cita c = adapter.buscarPorId(1L).orElseThrow();
        assertEquals(EstadoCita.FINALIZADA, c.getEstado());
        assertEquals("Calle 5", c.getDireccionServicio());
        assertEquals(4.1, c.getLatitud());
        assertEquals("ok", c.getDiagnostico());
        assertEquals(INICIO.plusHours(1), c.getFechaHoraFin());
        assertTrue(adapter.buscarPorId(2L).isEmpty());
    }

    @Test
    void buscarActivasExcluyeCanceladas() {
        LocalDateTime fin = INICIO.plusDays(1);
        when(jpa.buscarDelTecnicoEntre(2L, INICIO, fin, EstadoCitaEntity.CANCELADA))
                .thenReturn(List.of(entity(1L, EstadoCitaEntity.CONFIRMADA)));

        List<Cita> citas = adapter.buscarActivasDelTecnicoEntre(2L, INICIO, fin);

        assertEquals(1, citas.size());
        assertEquals(EstadoCita.CONFIRMADA, citas.get(0).getEstado());
    }

    @Test
    void buscarSolicitudesManualesPendientesConsultaPorEstadoPendiente() {
        when(jpa.buscarSolicitudesManualesPorEstado(EstadoCitaEntity.PENDIENTE))
                .thenReturn(List.of(entity(1L, EstadoCitaEntity.PENDIENTE), entity(2L, EstadoCitaEntity.PENDIENTE)));

        assertEquals(2, adapter.buscarSolicitudesManualesPendientes().size());
    }
}
