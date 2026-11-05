package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaFinalizadaConPago;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.EstadoCobro;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasFinalizadasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarCitasFinalizadasDelTecnicoUseCaseImplTest {

    private static final String CORREO = "tec@mail.com";

    private final ResolverTecnicoAutenticado resolverTecnico = mock(ResolverTecnicoAutenticado.class);
    private final ConsultaCitasFinalizadasDelTecnicoPort consultaCitas = mock(ConsultaCitasFinalizadasDelTecnicoPort.class);
    private final ConsultaClientesPort consultaClientes = mock(ConsultaClientesPort.class);
    private final CitaFotoRepositoryPort fotos = mock(CitaFotoRepositoryPort.class);
    private final PagoRepositoryPort pagoRepo = mock(PagoRepositoryPort.class);

    private final ConsultarCitasFinalizadasDelTecnicoUseCaseImpl useCase = new ConsultarCitasFinalizadasDelTecnicoUseCaseImpl(
            resolverTecnico, consultaCitas, consultaClientes, fotos, pagoRepo);

    private Cita cita(Long id, Long clienteId) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setClienteId(clienteId);
        return cita;
    }

    private Pago pago(Long citaId, String montoFinal, EstadoPago estado) {
        Pago pago = Pago.nuevo(citaId);
        pago.setMontoFinal(montoFinal == null ? null : new BigDecimal(montoFinal));
        pago.setEstado(estado);
        return pago;
    }

    @Test
    void listaLasFinalizadasConElEstadoDeCobroEnElMismoOrdenDelPuerto() {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(3L);
        when(resolverTecnico.resolver(CORREO)).thenReturn(tecnico);
        when(consultaCitas.buscarFinalizadas(3L, 50))
                .thenReturn(List.of(cita(12L, 1L), cita(11L, 2L), cita(10L, 1L), cita(9L, 2L)));
        when(consultaClientes.resumenesPorId(any())).thenReturn(Map.of(1L, new ClienteResumen("Ana", "Ruiz", "300", null)));
        when(fotos.listarPorCitas(any())).thenReturn(Map.of());
        when(pagoRepo.buscarPorCitaIds(any())).thenReturn(List.of(
                pago(12L, null, EstadoPago.PENDIENTE),
                pago(11L, "80000", EstadoPago.PENDIENTE),
                pago(10L, "80000", EstadoPago.PAGADO)));

        List<CitaFinalizadaConPago> r = useCase.consultar(CORREO);

        assertEquals(List.of(12L, 11L, 10L, 9L), r.stream().map(c -> c.cita().cita().getId()).toList());
        assertEquals(List.of(EstadoCobro.SIN_COSTO_FINAL, EstadoCobro.PENDIENTE, EstadoCobro.PAGADO,
                EstadoCobro.SIN_COSTO_FINAL), r.stream().map(CitaFinalizadaConPago::cobro).toList());
        assertEquals("Ana", r.get(0).cita().cliente().nombre());
        assertNull(r.get(1).cita().cliente());
        assertTrue(r.get(0).cita().fotos().isEmpty());
    }

    @Test
    void sinCitasFinalizadasDevuelveUnaListaVacia() {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(3L);
        when(resolverTecnico.resolver(CORREO)).thenReturn(tecnico);
        when(consultaCitas.buscarFinalizadas(3L, 50)).thenReturn(List.of());
        when(consultaClientes.resumenesPorId(any())).thenReturn(Map.of());
        when(fotos.listarPorCitas(any())).thenReturn(Map.of());
        when(pagoRepo.buscarPorCitaIds(any())).thenReturn(List.of());

        assertTrue(useCase.consultar(CORREO).isEmpty());
        verify(consultaCitas).buscarFinalizadas(3L, ConsultarCitasFinalizadasDelTecnicoUseCaseImpl.LIMITE);
    }
}
