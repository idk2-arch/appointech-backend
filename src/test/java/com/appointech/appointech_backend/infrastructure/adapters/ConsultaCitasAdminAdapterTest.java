package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaAdminFila;
import com.appointech.appointech_backend.infrastructure.repositories.CitasAdminJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaCitasAdminAdapterTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 7, 6, 8, 30);

    private CitasAdminJpaRepository jpa;
    private ConsultaCitasAdminAdapter adapter;

    @BeforeEach
    void preparar() {
        jpa = mock(CitasAdminJpaRepository.class);
        adapter = new ConsultaCitasAdminAdapter(jpa);
    }

    @SuppressWarnings("unchecked")
    private void devolver(Page<CitaAdminFila> pagina) {
        when(jpa.buscar(anyBoolean(), anyLong(), any(Collection.class), any(), any(), any(Pageable.class)))
                .thenReturn(pagina);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinFiltrosConsultaTodosLosTecnicosEstadosYFechas() {
        devolver(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginaResultado<CitaResumenAdmin> r = adapter.buscar(new FiltroCitasAdmin(null, null, null, null, 0, 10));

        ArgumentCaptor<Collection<EstadoCitaEntity>> estados = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jpa).buscar(org.mockito.ArgumentMatchers.eq(true), anyLong(), estados.capture(),
                desde.capture(), hasta.capture(), org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 10)));
        assertEquals(EstadoCitaEntity.values().length, estados.getValue().size());
        assertTrue(desde.getValue().isBefore(LocalDateTime.of(2000, 1, 1, 0, 0)));
        assertTrue(hasta.getValue().isAfter(LocalDateTime.of(3000, 1, 1, 0, 0)));
        assertTrue(r.contenido().isEmpty());
        assertEquals(0, r.totalElementos());
        assertEquals(0, r.totalPaginas());
    }

    @Test
    @SuppressWarnings("unchecked")
    void conFiltrosLosTraduceYHaceInclusivaLaFechaHasta() {
        devolver(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        adapter.buscar(new FiltroCitasAdmin(7L, EstadoCita.EN_CAMINO,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 1, 5));

        ArgumentCaptor<Collection<EstadoCitaEntity>> estados = ArgumentCaptor.forClass(Collection.class);
        verify(jpa).buscar(org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(7L), estados.capture(),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(1, 5)));
        assertEquals(List.of(EstadoCitaEntity.EN_CAMINO), List.copyOf(estados.getValue()));
    }

    @Test
    void mapeaFilasNombresTotalesYCitaSinTecnico() {
        CitaAdminFila conTecnico = new CitaAdminFila(1L, INICIO, INICIO.plusHours(2), EstadoCitaEntity.CONFIRMADA,
                "Ana", "Gómez", "Luis", "Pérez", "Plomería", "Calle 1");
        CitaAdminFila sinTecnico = new CitaAdminFila(2L, INICIO.minusDays(1), INICIO.minusDays(1).plusHours(1),
                EstadoCitaEntity.PENDIENTE, "Ana", "Gómez", null, null, "Electricidad", "Calle 2");
        devolver(new PageImpl<>(List.of(conTecnico, sinTecnico), PageRequest.of(0, 2), 5));

        PaginaResultado<CitaResumenAdmin> r = adapter.buscar(new FiltroCitasAdmin(null, null, null, null, 0, 2));

        assertEquals(0, r.pagina());
        assertEquals(2, r.tamanio());
        assertEquals(5, r.totalElementos());
        assertEquals(3, r.totalPaginas());
        CitaResumenAdmin a = r.contenido().get(0);
        assertEquals(1L, a.id());
        assertEquals(INICIO, a.fechaHoraInicio());
        assertEquals(INICIO.plusHours(2), a.fechaHoraFin());
        assertEquals(EstadoCita.CONFIRMADA, a.estado());
        assertEquals("Ana Gómez", a.nombreCliente());
        assertEquals("Luis Pérez", a.nombreTecnico());
        assertEquals("Plomería", a.servicio());
        assertEquals("Calle 1", a.direccion());
        CitaResumenAdmin b = r.contenido().get(1);
        assertNull(b.nombreTecnico());
        assertEquals(EstadoCita.PENDIENTE, b.estado());
        assertFalse(r.contenido().isEmpty());
    }
}
