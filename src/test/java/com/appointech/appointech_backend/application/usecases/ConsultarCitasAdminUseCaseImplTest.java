package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasAdminPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarCitasAdminUseCaseImplTest {

    private ConsultaCitasAdminPort port;
    private ConsultarCitasAdminUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        port = mock(ConsultaCitasAdminPort.class);
        useCase = new ConsultarCitasAdminUseCaseImpl(port);
    }

    @Test
    void conFiltrosValidosDelegaEnElPuertoYDevuelveSuPagina() {
        FiltroCitasAdmin filtro = new FiltroCitasAdmin(3L, EstadoCita.CONFIRMADA,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 2, 50);
        PaginaResultado<CitaResumenAdmin> pagina = new PaginaResultado<>(List.of(), 2, 50, 120, 3);
        when(port.buscar(filtro)).thenReturn(pagina);

        assertSame(pagina, useCase.consultar(filtro));
        verify(port).buscar(filtro);
    }

    @Test
    void aceptaFiltrosVaciosYFechasIguales() {
        FiltroCitasAdmin sinFiltros = new FiltroCitasAdmin(null, null, null, null, 0, 10);
        FiltroCitasAdmin mismoDia = new FiltroCitasAdmin(null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), 0, 1);
        PaginaResultado<CitaResumenAdmin> vacia = new PaginaResultado<>(List.of(), 0, 10, 0, 0);
        when(port.buscar(sinFiltros)).thenReturn(vacia);
        when(port.buscar(mismoDia)).thenReturn(vacia);

        assertEquals(0, useCase.consultar(sinFiltros).totalElementos());
        assertEquals(0, useCase.consultar(mismoDia).totalElementos());
    }

    @Test
    void fechaDesdePosteriorAFechaHastaEsInvalida() {
        FiltroCitasAdmin filtro = new FiltroCitasAdmin(null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1), 0, 10);

        assertThrows(FiltroCitasInvalidoException.class, () -> useCase.consultar(filtro));
        verifyNoInteractions(port);
    }

    @Test
    void paginaNegativaEsInvalida() {
        assertThrows(FiltroCitasInvalidoException.class,
                () -> useCase.consultar(new FiltroCitasAdmin(null, null, null, null, -1, 10)));
        verifyNoInteractions(port);
    }

    @Test
    void tamanioFueraDeRangoEsInvalido() {
        assertThrows(FiltroCitasInvalidoException.class,
                () -> useCase.consultar(new FiltroCitasAdmin(null, null, null, null, 0, 0)));
        assertThrows(FiltroCitasInvalidoException.class,
                () -> useCase.consultar(new FiltroCitasAdmin(null, null, null, null, 0, 51)));
        verifyNoInteractions(port);
    }
}
