package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.domain.ports.out.ConsultaHistorialPagosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El caso de uso valida y completa los criterios; el orden, el rango de fechas inclusivo y la exclusión de pagos
 * sin fecha los resuelve la consulta del puerto de salida (probada contra PostgreSQL y en el adaptador).
 */
class ConsultarHistorialPagosUseCaseImplTest {

    private ConsultaHistorialPagosPort puerto;
    private ConsultarHistorialPagosUseCaseImpl casoDeUso;

    @BeforeEach
    void preparar() {
        puerto = mock(ConsultaHistorialPagosPort.class);
        casoDeUso = new ConsultarHistorialPagosUseCaseImpl(puerto);
        when(puerto.buscar(any())).thenReturn(new ListadoConTotal<>(List.of(), 0));
    }

    private static FiltroHistorialPagos filtro(EstadoPago estado, MetodoPago metodo, Long clienteId, Long tecnicoId,
                                               LocalDate desde, LocalDate hasta, Integer pagina, Integer tamanio) {
        return new FiltroHistorialPagos(estado, metodo, clienteId, tecnicoId, desde, hasta, pagina, tamanio);
    }

    private FiltroHistorialPagos filtroEnviadoAlPuerto() {
        ArgumentCaptor<FiltroHistorialPagos> captor = ArgumentCaptor.forClass(FiltroHistorialPagos.class);
        verify(puerto).buscar(captor.capture());
        return captor.getValue();
    }

    @Test
    void sinPageNiSizeDevuelveLaPrimeraPaginaDeVeinteYNuncaTodo() {
        casoDeUso.consultar(filtro(null, null, null, null, null, null, null, null));

        FiltroHistorialPagos f = filtroEnviadoAlPuerto();
        assertEquals(0, f.pagina());
        assertEquals(20, f.tamanio());
    }

    @Test
    void conPageYSizeRespetaLaPaginaPedida() {
        casoDeUso.consultar(filtro(null, null, null, null, null, null, 3, 50));

        FiltroHistorialPagos f = filtroEnviadoAlPuerto();
        assertEquals(3, f.pagina());
        assertEquals(50, f.tamanio());
    }

    @Test
    void conUnoSoloElOtroTomaSuValorPorDefecto() {
        casoDeUso.consultar(filtro(null, null, null, null, null, null, 2, null));

        FiltroHistorialPagos f = filtroEnviadoAlPuerto();
        assertEquals(2, f.pagina());
        assertEquals(20, f.tamanio());
    }

    @Test
    void pasaCadaFiltroSinCambiarlo() {
        LocalDate desde = LocalDate.of(2030, 5, 1);
        LocalDate hasta = LocalDate.of(2030, 5, 31);

        casoDeUso.consultar(filtro(EstadoPago.PAGADO, MetodoPago.EN_LINEA, 4L, 7L, desde, hasta, null, null));

        FiltroHistorialPagos f = filtroEnviadoAlPuerto();
        assertEquals(EstadoPago.PAGADO, f.estado());
        assertEquals(MetodoPago.EN_LINEA, f.metodo());
        assertEquals(4L, f.clienteId());
        assertEquals(7L, f.tecnicoId());
        assertEquals(desde, f.desde());
        assertEquals(hasta, f.hasta());
    }

    @Test
    void devuelveLoQueEntregaElPuertoConElTotalCompleto() {
        ListadoConTotal<PagoDelHistorial> respuesta = new ListadoConTotal<>(List.of(new PagoDelHistorial(
                10L, 2L, "Ana Ruiz", 3L, "Luis Mora", "Plomería", new BigDecimal("100000.00"), new BigDecimal("95000.00"),
                MetodoPago.EFECTIVO, EstadoPago.PAGADO, LocalDateTime.of(2030, 5, 10, 9, 0), "COMP-000001")), 41);
        when(puerto.buscar(any())).thenReturn(respuesta);

        assertSame(respuesta, casoDeUso.consultar(filtro(null, null, null, null, null, null, 1, 1)));
    }

    @Test
    void unRangoDeUnSoloDiaEsValido() {
        LocalDate dia = LocalDate.of(2030, 5, 10);

        casoDeUso.consultar(filtro(null, null, null, null, dia, dia, null, null));

        FiltroHistorialPagos f = filtroEnviadoAlPuerto();
        assertEquals(dia, f.desde());
        assertEquals(dia, f.hasta());
    }

    @Test
    void desdePosteriorAHastaEs400() {
        FiltroHistorialPagos invertido = filtro(null, null, null, null, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 5, 1), null, null);

        var e = assertThrows(FiltroCitasInvalidoException.class, () -> casoDeUso.consultar(invertido));

        assertEquals("desde no puede ser posterior a hasta", e.getMessage());
        verify(puerto, never()).buscar(any());
    }

    @Test
    void paginaNegativaOTamanioFueraDeRangoSon400() {
        assertThrows(FiltroCitasInvalidoException.class,
                () -> casoDeUso.consultar(filtro(null, null, null, null, null, null, -1, 10)));
        assertThrows(FiltroCitasInvalidoException.class,
                () -> casoDeUso.consultar(filtro(null, null, null, null, null, null, 0, 0)));
        assertThrows(FiltroCitasInvalidoException.class,
                () -> casoDeUso.consultar(filtro(null, null, null, null, null, null, 0, 101)));
        verify(puerto, never()).buscar(any());
    }
}
