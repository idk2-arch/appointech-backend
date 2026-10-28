package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.infrastructure.entities.CitaFotoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaFotoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CitaFotoRepositoryAdapterTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 8, 30);

    private CitaFotoJpaRepository jpa;
    private CitaFotoRepositoryAdapter adaptador;

    @BeforeEach
    void preparar() {
        jpa = mock(CitaFotoJpaRepository.class);
        adaptador = new CitaFotoRepositoryAdapter(jpa);
    }

    private CitaFotoEntity entidad(Long id, Long citaId) {
        return new CitaFotoEntity(id, citaId, "citas/" + citaId + "/k" + id, AHORA);
    }

    @Test
    void guardaYDevuelveLaFotoConSuIdGenerado() {
        when(jpa.save(org.mockito.ArgumentMatchers.any(CitaFotoEntity.class))).thenReturn(entidad(9L, 20L));

        CitaFoto guardada = adaptador.guardar(new CitaFoto(null, 20L, "citas/20/k9", AHORA));

        ArgumentCaptor<CitaFotoEntity> captor = ArgumentCaptor.forClass(CitaFotoEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(20L, captor.getValue().getCitaId());
        assertEquals("citas/20/k9", captor.getValue().getClave());
        assertEquals(AHORA, captor.getValue().getCreadoEn());
        assertEquals(9L, guardada.id());
        assertEquals(20L, guardada.citaId());
    }

    @Test
    void buscaPorIdDentroDeLaCita() {
        when(jpa.findByIdAndCitaId(9L, 20L)).thenReturn(Optional.of(entidad(9L, 20L)));
        when(jpa.findByIdAndCitaId(9L, 21L)).thenReturn(Optional.empty());

        assertEquals("citas/20/k9", adaptador.buscarPorIdYCita(9L, 20L).orElseThrow().clave());
        assertTrue(adaptador.buscarPorIdYCita(9L, 21L).isEmpty());
    }

    @Test
    void listaLasFotosDeUnaCita() {
        when(jpa.findByCitaIdOrderByIdAsc(20L)).thenReturn(List.of(entidad(1L, 20L), entidad(2L, 20L)));

        assertEquals(List.of(1L, 2L), adaptador.listarPorCita(20L).stream().map(CitaFoto::id).toList());
    }

    @Test
    void listaLasFotosDeVariasCitasEnUnaSolaConsultaAgrupadasPorCita() {
        when(jpa.findByCitaIdInOrderByIdAsc(Set.of(20L, 21L)))
                .thenReturn(List.of(entidad(1L, 20L), entidad(2L, 21L), entidad(3L, 20L)));

        Map<Long, List<CitaFoto>> porCita = adaptador.listarPorCitas(Set.of(20L, 21L));

        assertEquals(List.of(1L, 3L), porCita.get(20L).stream().map(CitaFoto::id).toList());
        assertEquals(List.of(2L), porCita.get(21L).stream().map(CitaFoto::id).toList());
    }

    @Test
    void sinCitasNoConsultaLaBaseDeDatos() {
        assertTrue(adaptador.listarPorCitas(Set.of()).isEmpty());

        verifyNoInteractions(jpa);
    }

    @Test
    void cuentaYElimina() {
        when(jpa.countByCitaId(20L)).thenReturn(2L);

        assertEquals(2L, adaptador.contarPorCita(20L));
        adaptador.eliminar(9L);

        verify(jpa).deleteById(9L);
    }
}
