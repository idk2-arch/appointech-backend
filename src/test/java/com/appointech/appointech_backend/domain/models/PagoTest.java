package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.exception.PagoInvalidoException;
import com.appointech.appointech_backend.domain.exception.TransaccionNoCoincideException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final BigDecimal UMBRAL = BigDecimal.valueOf(20);

    private Pago pagoConCostoFinal(String monto) {
        Pago pago = Pago.nuevo(10L);
        pago.setId(3L);
        pago.registrarCostoFinal(new BigDecimal(monto));
        return pago;
    }

    private Pago pagoEnLinea() {
        Pago pago = pagoConCostoFinal("80000");
        pago.registrarIntentoEnLinea(pago.proximaReferencia());
        return pago;
    }

    private ResultadoTransaccion transaccion(String referencia, long centavos, EstadoTransaccionPasarela estado) {
        return new ResultadoTransaccion("tx-1", referencia, centavos, estado);
    }

    // ---------- creación y cotización ----------

    @Test
    void unPagoNuevoQuedaPendienteYSinMontos() {
        Pago pago = Pago.nuevo(10L);

        assertEquals(10L, pago.getCitaId());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNull(pago.getMontoFinal());
        assertNull(pago.getMetodo());
        assertEquals(0, pago.getIntentosPago());
    }

    @Test
    void laCotizacionSeGuardaConDosDecimalesYSePuedeActualizarAntesDelCostoFinal() {
        Pago pago = Pago.nuevo(10L);

        pago.registrarCotizacion(new BigDecimal("80000"));
        pago.registrarCotizacion(new BigDecimal("90000.456"));

        assertEquals(new BigDecimal("90000.46"), pago.getMontoCotizado());
    }

    @Test
    void laCotizacionQuedaCongeladaConCostoFinalOPagoRealizado() {
        Pago pago = Pago.nuevo(10L);
        pago.registrarCotizacion(new BigDecimal("80000"));
        pago.registrarCostoFinal(new BigDecimal("85000"));

        pago.registrarCotizacion(new BigDecimal("1"));
        assertEquals(new BigDecimal("80000.00"), pago.getMontoCotizado());

        Pago pagado = Pago.nuevo(11L);
        pagado.setEstado(EstadoPago.PAGADO);
        pagado.registrarCotizacion(new BigDecimal("1"));
        assertNull(pagado.getMontoCotizado());
    }

    // ---------- costo final ----------

    @Test
    void elCostoFinalSeRedondeaADosDecimales() {
        assertEquals(new BigDecimal("85000.50"), pagoConCostoFinal("85000.499").getMontoFinal());
        assertEquals(new BigDecimal("85000.00"), pagoConCostoFinal("85000").getMontoFinal());
    }

    @Test
    void elCostoFinalDebeSerMayorQueCero() {
        Pago pago = Pago.nuevo(10L);

        assertThrows(DatosPagoInvalidosException.class, () -> pago.registrarCostoFinal(null));
        assertThrows(DatosPagoInvalidosException.class, () -> pago.registrarCostoFinal(BigDecimal.ZERO));
        assertThrows(DatosPagoInvalidosException.class, () -> pago.registrarCostoFinal(new BigDecimal("-1")));
    }

    @Test
    void elCostoFinalNoPuedeSuperarElMaximoDeLaColumna() {
        Pago pago = Pago.nuevo(10L);

        assertThrows(DatosPagoInvalidosException.class, () -> pago.registrarCostoFinal(new BigDecimal("10000000000")));
    }

    @Test
    void unPagoPagadoNoAceptaOtroCostoFinal() {
        Pago pago = pagoConCostoFinal("80000");
        pago.usarEfectivo();
        pago.confirmarEfectivo(AHORA);

        assertThrows(PagoInvalidoException.class, () -> pago.registrarCostoFinal(new BigDecimal("1000")));
    }

    @Test
    void conUnPagoEnLineaEnCursoNoSePuedeCambiarElCostoFinal() {
        Pago pago = pagoEnLinea();

        assertThrows(PagoInvalidoException.class, () -> pago.registrarCostoFinal(new BigDecimal("1000")));
    }

    @Test
    void trasUnPagoEnLineaFallidoSiSePuedeCorregirElCostoFinal() {
        Pago pago = pagoEnLinea();
        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.RECHAZADA), AHORA);

        pago.registrarCostoFinal(new BigDecimal("90000"));

        assertEquals(new BigDecimal("90000.00"), pago.getMontoFinal());
    }

    // ---------- método de pago ----------

    @Test
    void sinCostoFinalNoSePuedeElegirMetodo() {
        Pago pago = Pago.nuevo(10L);

        assertThrows(PagoInvalidoException.class, pago::usarEfectivo);
        assertThrows(PagoInvalidoException.class, () -> pago.registrarIntentoEnLinea("APT-10-3-1"));
    }

    @Test
    void elEfectivoDejaElPagoPendienteConElMetodo() {
        Pago pago = pagoConCostoFinal("80000");

        pago.usarEfectivo();

        assertEquals(MetodoPago.EFECTIVO, pago.getMetodo());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void unPagoPagadoNoCambiaDeMetodo() {
        Pago pago = pagoConCostoFinal("80000");
        pago.usarEfectivo();
        pago.confirmarEfectivo(AHORA);

        assertThrows(PagoInvalidoException.class, pago::usarEfectivo);
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
    }

    @Test
    void cadaIntentoEnLineaTieneUnaReferenciaNuevaYUnica() {
        Pago pago = pagoConCostoFinal("80000");

        String primera = pago.proximaReferencia();
        pago.registrarIntentoEnLinea(primera);
        String segunda = pago.proximaReferencia();

        assertEquals("APT-10-3-1", primera);
        assertEquals("APT-10-3-2", segunda);
        assertEquals(MetodoPago.EN_LINEA, pago.getMetodo());
        assertEquals(primera, pago.getReferenciaExterna());
        assertEquals(1, pago.getIntentosPago());
    }

    @Test
    void unNuevoIntentoLimpiaLaTransaccionAnteriorYVuelveAPendiente() {
        Pago pago = pagoEnLinea();
        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.RECHAZADA), AHORA);
        assertEquals(EstadoPago.FALLIDO, pago.getEstado());

        pago.registrarIntentoEnLinea(pago.proximaReferencia());

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertEquals("APT-10-3-2", pago.getReferenciaExterna());
        assertNull(pago.getIdTransaccionExterna());
        assertEquals(2, pago.getIntentosPago());
    }

    @Test
    void laReferenciaExigeQueElPagoEsteGuardado() {
        assertThrows(IllegalStateException.class, () -> Pago.nuevo(10L).proximaReferencia());
    }

    @Test
    void conUnPagoEnLineaPendienteNoSePuedeCambiarDeMetodoNiReiniciar() {
        Pago pago = pagoEnLinea();

        PagoInvalidoException aEfectivo = assertThrows(PagoInvalidoException.class, pago::usarEfectivo);
        assertThrows(PagoInvalidoException.class, () -> pago.registrarIntentoEnLinea(pago.proximaReferencia()));

        assertEquals("Hay un pago en línea en proceso; verifica su resultado antes de cambiar de método.",
                aEfectivo.getMessage());
        assertEquals(MetodoPago.EN_LINEA, pago.getMetodo());
        assertEquals("APT-10-3-1", pago.getReferenciaExterna());
        assertEquals(1, pago.getIntentosPago());
    }

    @Test
    void trasUnPagoEnLineaFallidoSiSePuedeCambiarAEfectivo() {
        Pago pago = pagoEnLinea();
        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.ANULADA), AHORA);

        pago.usarEfectivo();

        assertEquals(MetodoPago.EFECTIVO, pago.getMetodo());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    // ---------- efectivo ----------

    @Test
    void confirmarEfectivoMarcaPagadoConLaFecha() {
        Pago pago = pagoConCostoFinal("80000");
        pago.usarEfectivo();

        pago.confirmarEfectivo(AHORA);

        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(AHORA, pago.getPagadoEn());
    }

    @Test
    void confirmarEfectivoExigeQueElClienteHayaElegidoEfectivo() {
        Pago sinMetodo = pagoConCostoFinal("80000");
        Pago enLinea = pagoEnLinea();

        assertThrows(PagoInvalidoException.class, () -> sinMetodo.confirmarEfectivo(AHORA));
        assertThrows(PagoInvalidoException.class, () -> enLinea.confirmarEfectivo(AHORA));
    }

    @Test
    void confirmarEfectivoDosVecesFallaYNoCambiaLaFecha() {
        Pago pago = pagoConCostoFinal("80000");
        pago.usarEfectivo();
        pago.confirmarEfectivo(AHORA);

        assertThrows(PagoInvalidoException.class, () -> pago.confirmarEfectivo(AHORA.plusDays(1)));
        assertEquals(AHORA, pago.getPagadoEn());
    }

    // ---------- resultado de la pasarela ----------

    @Test
    void unaTransaccionAprobadaDejaElPagoPagadoYGuardaSuId() {
        Pago pago = pagoEnLinea();

        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.APROBADA), AHORA);

        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(AHORA, pago.getPagadoEn());
        assertEquals("tx-1", pago.getIdTransaccionExterna());
    }

    @Test
    void rechazadaErrorOAnuladaDejanElPagoFallido() {
        for (EstadoTransaccionPasarela estado : new EstadoTransaccionPasarela[]{
                EstadoTransaccionPasarela.RECHAZADA, EstadoTransaccionPasarela.ERROR, EstadoTransaccionPasarela.ANULADA}) {
            Pago pago = pagoEnLinea();

            pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, estado), AHORA);

            assertEquals(EstadoPago.FALLIDO, pago.getEstado());
            assertNull(pago.getPagadoEn());
        }
    }

    @Test
    void unaTransaccionPendienteDejaElPagoPendiente() {
        Pago pago = pagoEnLinea();

        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.PENDIENTE), AHORA);

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void unaTransaccionFallidaPuedeAprobarseDespues() {
        Pago pago = pagoEnLinea();
        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.RECHAZADA), AHORA);

        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.APROBADA), AHORA);

        assertEquals(EstadoPago.PAGADO, pago.getEstado());
    }

    @Test
    void unPagoPagadoNuncaCambia() {
        Pago pago = pagoEnLinea();
        pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 8_000_000, EstadoTransaccionPasarela.APROBADA), AHORA);

        pago.aplicarResultado(transaccion("OTRA", 1, EstadoTransaccionPasarela.RECHAZADA), AHORA.plusDays(1));

        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(AHORA, pago.getPagadoEn());
    }

    @Test
    void laReferenciaDeLaTransaccionDebeSerLaDelPago() {
        Pago pago = pagoEnLinea();

        assertThrows(TransaccionNoCoincideException.class,
                () -> pago.aplicarResultado(transaccion("APT-99-9-1", 8_000_000, EstadoTransaccionPasarela.APROBADA), AHORA));
        assertThrows(TransaccionNoCoincideException.class,
                () -> pago.aplicarResultado(transaccion(null, 8_000_000, EstadoTransaccionPasarela.APROBADA), AHORA));
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void elMontoDeLaTransaccionDebeSerElDelPago() {
        Pago pago = pagoEnLinea();

        assertThrows(TransaccionNoCoincideException.class,
                () -> pago.aplicarResultado(transaccion(pago.getReferenciaExterna(), 100, EstadoTransaccionPasarela.APROBADA), AHORA));
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        assertNull(pago.getIdTransaccionExterna());
    }

    @Test
    void sinPagoEnLineaIniciadoNoSeAplicaNingunResultado() {
        Pago efectivo = pagoConCostoFinal("80000");
        efectivo.usarEfectivo();
        Pago sinMetodo = Pago.nuevo(10L);
        ResultadoTransaccion resultado = transaccion("APT-10-3-1", 8_000_000, EstadoTransaccionPasarela.APROBADA);

        assertThrows(PagoInvalidoException.class, () -> efectivo.aplicarResultado(resultado, AHORA));
        assertThrows(PagoInvalidoException.class, () -> sinMetodo.aplicarResultado(resultado, AHORA));
    }

    // ---------- diferencia entre cotizado y final ----------

    private Pago conMontos(String cotizado, String finalMonto) {
        Pago pago = Pago.nuevo(10L);
        if (cotizado != null) {
            pago.registrarCotizacion(new BigDecimal(cotizado));
        }
        if (finalMonto != null) {
            pago.registrarCostoFinal(new BigDecimal(finalMonto));
        }
        return pago;
    }

    @Test
    void laDiferenciaSeCalculaEnPorcentajeSobreLoCotizado() {
        assertEquals(new BigDecimal("25.00"), conMontos("80000", "100000").porcentajeDiferencia());
        assertEquals(new BigDecimal("25.00"), conMontos("80000", "60000").porcentajeDiferencia());
        assertEquals(new BigDecimal("0.00"), conMontos("80000", "80000").porcentajeDiferencia());
    }

    @Test
    void esSignificativaSoloSiSuperaElUmbral() {
        assertTrue(conMontos("80000", "100000").diferenciaSignificativa(UMBRAL));
        assertTrue(conMontos("80000", "50000").diferenciaSignificativa(UMBRAL));
        assertFalse(conMontos("80000", "90000").diferenciaSignificativa(UMBRAL));
    }

    @Test
    void justoEnElUmbralNoEsSignificativa() {
        assertFalse(conMontos("100000", "120000").diferenciaSignificativa(UMBRAL));
        assertTrue(conMontos("100000", "120000.01").diferenciaSignificativa(UMBRAL));
    }

    @Test
    void sinCotizacionOSinCostoFinalNoHayDiferencia() {
        for (Pago pago : new Pago[]{conMontos(null, "100000"), conMontos("80000", null), conMontos(null, null)}) {
            assertNull(pago.porcentajeDiferencia());
            assertFalse(pago.diferenciaSignificativa(UMBRAL));
        }
    }

    @Test
    void unaCotizacionEnCeroNoSePuedeUsarComoBase() {
        Pago pago = Pago.nuevo(10L);
        pago.setMontoCotizado(BigDecimal.ZERO);
        pago.setMontoFinal(new BigDecimal("1000"));

        assertNull(pago.porcentajeDiferencia());
        assertFalse(pago.diferenciaSignificativa(UMBRAL));
    }

    // ---------- acceso a los demás campos ----------

    @Test
    void losCamposSeLeenYEscribenTalCual() {
        Pago pago = new Pago();
        pago.setId(1L);
        pago.setCitaId(2L);
        pago.setMontoCotizado(BigDecimal.ONE);
        pago.setMontoFinal(BigDecimal.TEN);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setEstado(EstadoPago.FALLIDO);
        pago.setReferenciaExterna("r");
        pago.setIdTransaccionExterna("t");
        pago.setIntentosPago(4);
        pago.setPagadoEn(AHORA);

        assertEquals(1L, pago.getId());
        assertEquals(2L, pago.getCitaId());
        assertEquals(BigDecimal.ONE, pago.getMontoCotizado());
        assertEquals(BigDecimal.TEN, pago.getMontoFinal());
        assertEquals(MetodoPago.EFECTIVO, pago.getMetodo());
        assertEquals(EstadoPago.FALLIDO, pago.getEstado());
        assertEquals("r", pago.getReferenciaExterna());
        assertEquals("t", pago.getIdTransaccionExterna());
        assertEquals(4, pago.getIntentosPago());
        assertEquals(AHORA, pago.getPagadoEn());
    }

    @Test
    void elEstadoDeCobroDistingueSinCostoPendienteYPagado() {
        Pago pago = Pago.nuevo(10L);
        assertEquals(EstadoCobro.SIN_COSTO_FINAL, pago.estadoCobro());

        pago.registrarCostoFinal(new BigDecimal("80000"));
        assertEquals(EstadoCobro.PENDIENTE, pago.estadoCobro());

        pago.setEstado(EstadoPago.PAGADO);
        assertEquals(EstadoCobro.PAGADO, pago.estadoCobro());
    }

    @Test
    void exigirPagoEnLineaIniciadoRechazaEfectivoYPagosSinReferencia() {
        Pago pago = Pago.nuevo(10L);
        assertThrows(PagoInvalidoException.class, pago::exigirPagoEnLineaIniciado);

        pago.setMetodo(MetodoPago.EN_LINEA);
        assertThrows(PagoInvalidoException.class, pago::exigirPagoEnLineaIniciado);

        pago.setReferenciaExterna("APT-10-1-1");
        pago.exigirPagoEnLineaIniciado();
    }
}
