package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.FiltroHistorialPagos;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.PagoDelHistorial;
import com.appointech.appointech_backend.infrastructure.dto.PagoHistorialResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistorialPagosMapperTest {

    @Test
    void sinParametrosNoFiltraNiPagina() {
        assertEquals(new FiltroHistorialPagos(null, null, null, null, null, null, null, null),
                HistorialPagosMapper.aFiltro(null, null, null, null, null, null, null, null));
    }

    @Test
    void textoEnBlancoSeTrataComoSinFiltro() {
        assertEquals(new FiltroHistorialPagos(null, null, null, null, null, null, null, null),
                HistorialPagosMapper.aFiltro(" ", "", null, null, "  ", "", null, null));
    }

    @Test
    void conviertePasaYNormalizaMayusculasYEspacios() {
        assertEquals(new FiltroHistorialPagos(EstadoPago.FALLIDO, MetodoPago.EFECTIVO, 1L, 2L,
                        LocalDate.of(2030, 1, 5), LocalDate.of(2030, 2, 6), 3, 25),
                HistorialPagosMapper.aFiltro(" fallido ", "Efectivo", 1L, 2L, " 2030-01-05 ", "2030-02-06", 3, 25));
    }

    @Test
    void estadoYMetodoDesconocidosSonFiltrosInvalidosConMensajeLegible() {
        var estado = assertThrows(FiltroCitasInvalidoException.class,
                () -> HistorialPagosMapper.aFiltro("COBRADO", null, null, null, null, null, null, null));
        var metodo = assertThrows(FiltroCitasInvalidoException.class,
                () -> HistorialPagosMapper.aFiltro(null, "TARJETA", null, null, null, null, null, null));

        assertEquals("Valor inválido para estado: COBRADO", estado.getMessage());
        assertEquals("Valor inválido para metodo: TARJETA", metodo.getMessage());
    }

    @Test
    void fechasMalEscritasSonFiltrosInvalidos() {
        var desde = assertThrows(FiltroCitasInvalidoException.class,
                () -> HistorialPagosMapper.aFiltro(null, null, null, null, "2030/05/01", null, null, null));
        var hasta = assertThrows(FiltroCitasInvalidoException.class,
                () -> HistorialPagosMapper.aFiltro(null, null, null, null, null, "31-05-2030", null, null));
        var inexistente = assertThrows(FiltroCitasInvalidoException.class,
                () -> HistorialPagosMapper.aFiltro(null, null, null, null, "2030-02-31", null, null, null));

        assertEquals("desde debe tener el formato yyyy-MM-dd", desde.getMessage());
        assertEquals("hasta debe tener el formato yyyy-MM-dd", hasta.getMessage());
        assertEquals("desde debe tener el formato yyyy-MM-dd", inexistente.getMessage());
    }

    @Test
    void respuestaDeUnPagoCompletoTraeSoloLosCamposDelContrato() {
        PagoHistorialResponse r = HistorialPagosMapper.aRespuesta(new PagoDelHistorial(
                10L, 2L, "Ana Ruiz", 3L, "Luis Mora", "Plomería", new BigDecimal("100000.00"), new BigDecimal("95000.00"),
                MetodoPago.EN_LINEA, EstadoPago.PAGADO, LocalDateTime.of(2030, 5, 10, 9, 30), "COMP-000001"));

        assertEquals(new PagoHistorialResponse(10L, 2L, "Ana Ruiz", 3L, "Luis Mora", "Plomería",
                new BigDecimal("100000.00"), new BigDecimal("95000.00"), "EN_LINEA", "PAGADO",
                LocalDateTime.of(2030, 5, 10, 9, 30), "COMP-000001"), r);
        assertEquals(12, PagoHistorialResponse.class.getRecordComponents().length);
    }

    @Test
    void respuestaDeUnPagoPendienteSinComprobanteNiMetodoLosDejaEnNulo() {
        PagoHistorialResponse r = HistorialPagosMapper.aRespuesta(new PagoDelHistorial(
                11L, 2L, "Ana Ruiz", null, null, "Neveras", new BigDecimal("80000.00"), null, null,
                EstadoPago.PENDIENTE, null, null));

        assertEquals("PENDIENTE", r.estado());
        assertNull(r.metodo());
        assertNull(r.numeroComprobante());
        assertNull(r.pagadoEn());
        assertNull(r.tecnicoId());
        assertNull(r.montoFinal());
    }
}
