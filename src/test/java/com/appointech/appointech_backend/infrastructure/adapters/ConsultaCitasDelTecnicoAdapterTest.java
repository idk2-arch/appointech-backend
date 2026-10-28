package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitasDelTecnicoJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaCitasDelTecnicoAdapterTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 7, 6, 8, 0);
    private static final LocalDateTime FIN = INICIO.plusDays(1);

    @Mock
    private CitasDelTecnicoJpaRepository jpa;
    @InjectMocks
    private ConsultaCitasDelTecnicoAdapter adapter;

    @Test
    void traduceEstadosDeDominioAEntidadYMapeaElResultado() {
        CitaEntity e = new CitaEntity();
        e.setId(1L);
        e.setClienteId(2L);
        e.setTecnicoId(3L);
        e.setEspecialidadId(4L);
        e.setDescripcionProblema("fuga");
        e.setDireccionServicio("Calle 9");
        e.setLatitud(4.5);
        e.setLongitud(-74.0);
        e.setFechaHoraInicio(INICIO.plusHours(2));
        e.setFechaHoraFin(INICIO.plusHours(3));
        e.setEstado(EstadoCitaEntity.EN_CAMINO);
        e.setDiagnostico("d");
        e.setCreadaEn(INICIO.minusDays(2));
        e.setActualizadaEn(INICIO.minusDays(1));
        when(jpa.buscarPorEstadosEntre(3L, List.of(EstadoCitaEntity.EN_CAMINO), INICIO, FIN)).thenReturn(List.of(e));

        List<Cita> citas = adapter.buscarPorEstadosEntre(3L, List.of(EstadoCita.EN_CAMINO), INICIO, FIN);

        assertEquals(1, citas.size());
        Cita c = citas.get(0);
        assertEquals(EstadoCita.EN_CAMINO, c.getEstado());
        assertEquals(2L, c.getClienteId());
        assertEquals(4L, c.getEspecialidadId());
        assertEquals("Calle 9", c.getDireccionServicio());
        assertEquals(4.5, c.getLatitud());
        assertEquals(INICIO.plusHours(3), c.getFechaHoraFin());
        assertEquals("d", c.getDiagnostico());
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        when(jpa.buscarPorEstadosEntre(3L, List.of(EstadoCitaEntity.CONFIRMADA), INICIO, FIN)).thenReturn(List.of());

        assertTrue(adapter.buscarPorEstadosEntre(3L, Set.of(EstadoCita.CONFIRMADA), INICIO, FIN).isEmpty());
    }
}
