package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.infrastructure.entities.FranjaTecnicoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.FranjaTecnicoJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FranjaTrabajoRepositoryAdapterTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 5, 3);
    private static final LocalDateTime CREADA = LocalDateTime.of(2030, 5, 2, 10, 0);

    private final FranjaTecnicoJpaRepository jpa = mock(FranjaTecnicoJpaRepository.class);
    private final FranjaTrabajoRepositoryAdapter adapter = new FranjaTrabajoRepositoryAdapter(jpa);

    private static FranjaTecnicoEntity entidad(long id) {
        return new FranjaTecnicoEntity(id, 1L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0), 77L, CREADA);
    }

    private static final FranjaTrabajo DOMINIO = new FranjaTrabajo(5L, 1L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0), 77L, CREADA);

    @Test
    void guardaTodosLosCamposYDevuelveLaFranjaConSuId() {
        when(jpa.save(any(FranjaTecnicoEntity.class))).thenReturn(entidad(5));

        FranjaTrabajo guardada = adapter.guardar(new FranjaTrabajo(null, 1L, FECHA, LocalTime.of(8, 0), LocalTime.of(12, 0), 77L, CREADA));

        ArgumentCaptor<FranjaTecnicoEntity> captor = ArgumentCaptor.forClass(FranjaTecnicoEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(1L, captor.getValue().getTecnicoId());
        assertEquals(FECHA, captor.getValue().getFecha());
        assertEquals(LocalTime.of(12, 0), captor.getValue().getHoraFin());
        assertEquals(77L, captor.getValue().getCreadaPorId());
        assertEquals(DOMINIO, guardada);
    }

    @Test
    void buscarDelTecnicoSoloEncuentraLaDeEseTecnico() {
        when(jpa.findByIdAndTecnicoId(5L, 1L)).thenReturn(Optional.of(entidad(5)));
        when(jpa.findByIdAndTecnicoId(5L, 2L)).thenReturn(Optional.empty());

        assertEquals(Optional.of(DOMINIO), adapter.buscarDelTecnico(5L, 1L));
        assertTrue(adapter.buscarDelTecnico(5L, 2L).isEmpty());
    }

    @Test
    void lasConsultasPorFechaYPorRangoConviertenLasEntidades() {
        when(jpa.findByTecnicoIdAndFechaOrderByHoraInicioAsc(1L, FECHA)).thenReturn(List.of(entidad(5)));
        when(jpa.findByTecnicoIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(1L, FECHA, FECHA.plusDays(2)))
                .thenReturn(List.of(entidad(5)));
        when(jpa.findByTecnicoIdInAndFechaOrderByHoraInicioAsc(Set.of(1L, 2L), FECHA)).thenReturn(List.of(entidad(5)));

        assertEquals(List.of(DOMINIO), adapter.buscarDelTecnicoEnFecha(1L, FECHA));
        assertEquals(List.of(DOMINIO), adapter.buscarDelTecnicoEntre(1L, FECHA, FECHA.plusDays(2)));
        assertEquals(List.of(DOMINIO), adapter.buscarDeTecnicosEnFecha(Set.of(1L, 2L), FECHA));
    }

    @Test
    void sinTecnicosNoConsultaLaBase() {
        assertTrue(adapter.buscarDeTecnicosEnFecha(Set.of(), FECHA).isEmpty());
        verify(jpa, never()).findByTecnicoIdInAndFechaOrderByHoraInicioAsc(any(), any());
    }

    @Test
    void eliminaPorId() {
        adapter.eliminar(5L);

        verify(jpa).deleteById(5L);
    }
}
