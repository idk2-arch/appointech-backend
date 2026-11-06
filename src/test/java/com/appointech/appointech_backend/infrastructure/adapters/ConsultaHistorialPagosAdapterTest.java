package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.infrastructure.repositories.HistorialPagosJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.PagoHistorialFila;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaHistorialPagosAdapterTest {

    private static final LocalDateTime PAGADO = LocalDateTime.of(2030, 5, 10, 9, 30);

    @Mock
    private HistorialPagosJpaRepository jpa;
    @InjectMocks
    private ConsultaHistorialPagosAdapter adapter;

    private static PagoHistorialFila fila(Long comprobanteId) {
        return new PagoHistorialFila(10L, 2L, "Ana", "Ruiz", 3L, "Luis", "Mora", "Plomería",
                new BigDecimal("100000.00"), new BigDecimal("95000.00"), MetodoPago.EFECTIVO, EstadoPago.PAGADO,
                PAGADO, comprobanteId);
    }

    @Test
    void sinFiltrosUsaIndicadoresYCentinelasNoNulos() {
        when(jpa.buscar(eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(fila(null))));

        ListadoConTotal<PagoDelHistorial> r = adapter.buscar(
                new FiltroHistorialPagos(null, null, null, null, null, null, 0, 20));

        assertEquals(1, r.total());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jpa).buscar(eq(true), eq(EstadoPago.PENDIENTE), eq(true), eq(MetodoPago.EFECTIVO), eq(true), eq(-1L),
                eq(true), eq(-1L), eq(true), desde.capture(), hasta.capture(), pageable.capture());
        assertEquals(LocalDateTime.of(1900, 1, 1, 0, 0), desde.getValue());
        assertEquals(LocalDateTime.of(9999, 1, 1, 0, 0), hasta.getValue());
        assertEquals(PageRequest.of(0, 20), pageable.getValue());
    }

    @Test
    void conFiltrosLosEnviaTalCualYElHastaIncluyeTodoEseDia() {
        LocalDateTime inicioDia10 = LocalDateTime.of(2030, 5, 10, 0, 0);
        LocalDateTime inicioDia12 = LocalDateTime.of(2030, 5, 12, 0, 0);
        when(jpa.buscar(false, EstadoPago.PAGADO, false, MetodoPago.EN_LINEA, false, 4L, false, 7L, false,
                inicioDia10, inicioDia12, PageRequest.of(2, 5)))
                .thenReturn(new PageImpl<>(List.of(fila(8L)), PageRequest.of(2, 5), 11));

        ListadoConTotal<PagoDelHistorial> r = adapter.buscar(new FiltroHistorialPagos(EstadoPago.PAGADO,
                MetodoPago.EN_LINEA, 4L, 7L, LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 11), 2, 5));

        assertEquals(1, r.elementos().size());
        assertEquals(11, r.total());
    }

    @Test
    void conSoloDesdeElLimiteSuperiorNoRestringe() {
        when(jpa.buscar(eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), eq(false), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.buscar(new FiltroHistorialPagos(null, null, null, null, LocalDate.of(2030, 5, 10), null, 0, 20));

        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jpa).buscar(eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), eq(false),
                eq(LocalDateTime.of(2030, 5, 10, 0, 0)), hasta.capture(), any());
        assertEquals(LocalDateTime.of(9999, 1, 1, 0, 0), hasta.getValue());
    }

    @Test
    void convierteLaFilaEnDominioConNombresCompletosYNumeroDeComprobante() {
        when(jpa.buscar(eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(fila(8L))));

        PagoDelHistorial p = adapter.buscar(new FiltroHistorialPagos(null, null, null, null, null, null, 0, 20))
                .elementos().get(0);

        assertEquals(10L, p.citaId());
        assertEquals(2L, p.clienteId());
        assertEquals("Ana Ruiz", p.clienteNombre());
        assertEquals(3L, p.tecnicoId());
        assertEquals("Luis Mora", p.tecnicoNombre());
        assertEquals("Plomería", p.especialidad());
        assertEquals(new BigDecimal("100000.00"), p.montoCotizado());
        assertEquals(new BigDecimal("95000.00"), p.montoFinal());
        assertEquals(MetodoPago.EFECTIVO, p.metodo());
        assertEquals(EstadoPago.PAGADO, p.estado());
        assertEquals(PAGADO, p.pagadoEn());
        assertEquals("COMP-000008", p.numeroComprobante());
    }

    @Test
    void sinComprobanteNiTecnicoEsosCamposQuedanEnNulo() {
        PagoHistorialFila sinTecnico = new PagoHistorialFila(11L, 2L, "Ana", "Ruiz", null, null, null, "Plomería",
                new BigDecimal("100000.00"), null, null, EstadoPago.PENDIENTE, null, null);
        when(jpa.buscar(eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), eq(true), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sinTecnico)));

        PagoDelHistorial p = adapter.buscar(new FiltroHistorialPagos(null, null, null, null, null, null, 0, 20))
                .elementos().get(0);

        assertNull(p.tecnicoId());
        assertNull(p.tecnicoNombre());
        assertNull(p.numeroComprobante());
        assertNull(p.metodo());
        assertNull(p.pagadoEn());
        assertNull(p.montoFinal());
    }
}
