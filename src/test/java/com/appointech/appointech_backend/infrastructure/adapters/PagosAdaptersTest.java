package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.EstadoTransaccionPasarela;
import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.models.SolicitudPago;
import com.appointech.appointech_backend.infrastructure.entities.ComprobantePagoEntity;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.PagoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ComprobantePagoJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.PagoJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagosAdaptersTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 5, 10, 0);

    private PagoEntity entidadPago() {
        return new PagoEntity(1L, 10L, new BigDecimal("80000.00"), new BigDecimal("90000.00"), MetodoPago.EN_LINEA,
                EstadoPago.PAGADO, "APT-10-1-2", AHORA, "tx-9", 2);
    }

    private Pago pagoCompleto() {
        Pago p = new Pago();
        p.setId(1L);
        p.setCitaId(10L);
        p.setMontoCotizado(new BigDecimal("80000.00"));
        p.setMontoFinal(new BigDecimal("90000.00"));
        p.setMetodo(MetodoPago.EN_LINEA);
        p.setEstado(EstadoPago.PAGADO);
        p.setReferenciaExterna("APT-10-1-2");
        p.setPagadoEn(AHORA);
        p.setIdTransaccionExterna("tx-9");
        p.setIntentosPago(2);
        return p;
    }

    @Test
    void pagoRepositorioConvierteEntreEntidadYDominioSinPerderCampos() {
        PagoJpaRepository jpa = mock(PagoJpaRepository.class);
        PagoRepositoryAdapter adaptador = new PagoRepositoryAdapter(jpa);
        when(jpa.save(any(PagoEntity.class))).thenAnswer(i -> i.getArgument(0));

        Pago guardado = adaptador.guardar(pagoCompleto());

        ArgumentCaptor<PagoEntity> captor = ArgumentCaptor.forClass(PagoEntity.class);
        verify(jpa).save(captor.capture());
        assertEquals(entidadPago().getReferenciaExterna(), captor.getValue().getReferenciaExterna());
        assertEquals("tx-9", guardado.getIdTransaccionExterna());
        assertEquals(2, guardado.getIntentosPago());
        assertEquals(EstadoPago.PAGADO, guardado.getEstado());
        assertEquals(new BigDecimal("90000.00"), guardado.getMontoFinal());
        assertEquals(AHORA, guardado.getPagadoEn());
    }

    @Test
    void pagoRepositorioBuscaPorCitaConYSinBloqueo() {
        PagoJpaRepository jpa = mock(PagoJpaRepository.class);
        PagoRepositoryAdapter adaptador = new PagoRepositoryAdapter(jpa);
        when(jpa.findByCitaId(10L)).thenReturn(Optional.of(entidadPago()));
        when(jpa.buscarPorCitaIdBloqueando(10L)).thenReturn(Optional.of(entidadPago()));
        when(jpa.findByCitaId(11L)).thenReturn(Optional.empty());

        assertEquals(10L, adaptador.buscarPorCitaId(10L).orElseThrow().getCitaId());
        assertEquals(1L, adaptador.buscarPorCitaIdBloqueando(10L).orElseThrow().getId());
        assertTrue(adaptador.buscarPorCitaId(11L).isEmpty());
    }

    @Test
    void comprobanteRepositorioGuardaYBuscaPorPago() {
        ComprobantePagoJpaRepository jpa = mock(ComprobantePagoJpaRepository.class);
        ComprobantePagoRepositoryAdapter adaptador = new ComprobantePagoRepositoryAdapter(jpa);
        when(jpa.save(any(ComprobantePagoEntity.class)))
                .thenReturn(new ComprobantePagoEntity(4L, 1L, AHORA));
        when(jpa.findByPagoId(1L)).thenReturn(Optional.of(new ComprobantePagoEntity(4L, 1L, AHORA)));
        when(jpa.findByPagoId(2L)).thenReturn(Optional.empty());

        ComprobantePago guardado = adaptador.guardar(new ComprobantePago(null, 1L, AHORA));

        assertEquals("COMP-000004", guardado.numero());
        assertEquals(AHORA, adaptador.buscarPorPagoId(1L).orElseThrow().emitidoEn());
        assertTrue(adaptador.buscarPorPagoId(2L).isEmpty());
    }

    @Test
    void laEspecialidadConservaSuCostoDeReferenciaAlGuardarYLeer() {
        EspecialidadJpaRepository jpa = mock(EspecialidadJpaRepository.class);
        EspecialidadRepositoryAdapter adaptador = new EspecialidadRepositoryAdapter(jpa);
        when(jpa.save(any(EspecialidadEntity.class))).thenAnswer(i -> i.getArgument(0));
        Especialidad especialidad = new Especialidad(1L, "Lavadoras", "d", true);
        especialidad.setCostoReferencia(new BigDecimal("80000"));

        Especialidad guardada = adaptador.guardar(especialidad);

        assertEquals(new BigDecimal("80000"), guardada.getCostoReferencia());
    }

    // ---------- pasarela simulada ----------

    private SolicitudPago solicitud() {
        return new SolicitudPago("APT-10-1-1", 8_000_000L, "COP", "c@x.com", "http://localhost:4200/pago/resultado");
    }

    @Test
    void laSimuladaIniciaElPagoSinLlamarAfueraYDevuelveUnaUrlFicticiaConElIdDeTransaccion() {
        InicioPago inicio = new PasarelaPagoSimuladaAdapter(true).iniciarPago(solicitud());

        assertEquals("APT-10-1-1", inicio.referencia());
        assertTrue(inicio.urlPago().startsWith("http://localhost:4200/pago/resultado?id=SIM:APT-10-1-1:8000000"));
        assertTrue(inicio.urlPago().contains("referencia=APT-10-1-1"));
    }

    @Test
    void laSimuladaConsultaUnaTransaccionPropiaConReferenciaYMonto() {
        ResultadoTransaccion aprobada = new PasarelaPagoSimuladaAdapter(true).consultarTransaccion("SIM:APT-10-1-1:8000000");
        ResultadoTransaccion rechazada = new PasarelaPagoSimuladaAdapter(false).consultarTransaccion("SIM:APT-10-1-1:8000000");

        assertEquals("APT-10-1-1", aprobada.referencia());
        assertEquals(8_000_000L, aprobada.montoEnCentavos());
        assertEquals(EstadoTransaccionPasarela.APROBADA, aprobada.estado());
        assertEquals(EstadoTransaccionPasarela.RECHAZADA, rechazada.estado());
    }

    @Test
    void laSimuladaTrataUnIdAjenoOMalFormadoComoErrorSinReferencia() {
        PasarelaPagoSimuladaAdapter adaptador = new PasarelaPagoSimuladaAdapter(true);

        for (String id : new String[]{null, "WOMPI-123", "SIM:solo", "SIM:ref:abc", "SIM:a:1:2"}) {
            ResultadoTransaccion r = adaptador.consultarTransaccion(id);
            assertEquals(EstadoTransaccionPasarela.ERROR, r.estado());
            assertNull(r.referencia());
            assertEquals(0, r.montoEnCentavos());
        }
    }

    @Test
    void laSimuladaReconstruyeLaTransaccionDesdeLaReferenciaYElMontoEsperadoYRespetaElSwitch() {
        ResultadoTransaccion aprobada = new PasarelaPagoSimuladaAdapter(true).consultarPorReferencia("APT-10-1-1", 8_000_000L);
        ResultadoTransaccion rechazada = new PasarelaPagoSimuladaAdapter(false).consultarPorReferencia("APT-10-1-1", 8_000_000L);

        assertEquals("SIM:APT-10-1-1:8000000", aprobada.idTransaccion());
        assertEquals("APT-10-1-1", aprobada.referencia());
        assertEquals(8_000_000L, aprobada.montoEnCentavos());
        assertEquals(EstadoTransaccionPasarela.APROBADA, aprobada.estado());
        assertEquals(EstadoTransaccionPasarela.RECHAZADA, rechazada.estado());
    }

    @Test
    void elAdaptadorDePagosBuscaPorVariasCitas() {
        PagoJpaRepository jpa = mock(PagoJpaRepository.class);
        PagoEntity entidad = new PagoEntity(1L, 10L, null, new BigDecimal("80000.00"), MetodoPago.EFECTIVO,
                EstadoPago.PENDIENTE, null, null, null, 0);
        when(jpa.findByCitaIdIn(java.util.List.of(10L, 11L))).thenReturn(java.util.List.of(entidad));

        var pagos = new PagoRepositoryAdapter(jpa).buscarPorCitaIds(java.util.List.of(10L, 11L));

        assertEquals(1, pagos.size());
        assertEquals(10L, pagos.get(0).getCitaId());
        assertEquals(MetodoPago.EFECTIVO, pagos.get(0).getMetodo());
    }

    @Test
    void laReservaDeWompiResponde503ParaQueSoloElEfectivoSigaDisponible() {
        PasarelaPagoWompiPendienteAdapter adaptador = new PasarelaPagoWompiPendienteAdapter();

        assertThrows(PasarelaNoDisponibleException.class, () -> adaptador.iniciarPago(solicitud()));
        assertThrows(PasarelaNoDisponibleException.class, () -> adaptador.consultarTransaccion("x"));
        assertThrows(PasarelaNoDisponibleException.class, () -> adaptador.consultarPorReferencia("APT-10-1-1", 1L));
    }
}
