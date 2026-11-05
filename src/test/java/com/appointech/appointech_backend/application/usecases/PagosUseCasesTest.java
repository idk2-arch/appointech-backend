package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EmisorComprobante;
import com.appointech.appointech_backend.application.services.GestorCotizacion;
import com.appointech.appointech_backend.application.services.PagosDeCita;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.PagoInvalidoException;
import com.appointech.appointech_backend.domain.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.TransaccionNoCoincideException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ComprobanteDetalle;
import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.EstadoTransaccionPasarela;
import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoMetodoPago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Casos de uso del módulo de pagos con puertos simulados; el verificador de acceso también es simulado. */
class PagosUseCasesTest {

    private static final Long CITA_ID = 10L;
    private static final String CLIENTE = "cliente@x.com";
    private static final String TECNICO = "tecnico@x.com";
    private static final Instant INSTANTE = Instant.parse("2026-11-05T15:00:00Z");
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private VerificadorAccesoCita verificador;
    private PagoRepositoryPort pagoRepo;
    private ComprobantePagoRepositoryPort comprobanteRepo;
    private EspecialidadRepositoryPort especialidadRepo;
    private ClienteRepositoryPort clienteRepo;
    private TecnicoRepositoryPort tecnicoRepo;
    private PasarelaPagoPort pasarela;
    private RegistroLogPort registroLog;
    private PagosDeCita pagos;
    private EmisorComprobante emisor;
    private GestorCotizacion gestorCotizacion;
    private Clock reloj;
    private Cita cita;

    @BeforeEach
    void preparar() {
        verificador = mock(VerificadorAccesoCita.class);
        pagoRepo = mock(PagoRepositoryPort.class);
        comprobanteRepo = mock(ComprobantePagoRepositoryPort.class);
        especialidadRepo = mock(EspecialidadRepositoryPort.class);
        clienteRepo = mock(ClienteRepositoryPort.class);
        tecnicoRepo = mock(TecnicoRepositoryPort.class);
        pasarela = mock(PasarelaPagoPort.class);
        registroLog = mock(RegistroLogPort.class);
        pagos = new PagosDeCita(pagoRepo, registroLog);
        emisor = new EmisorComprobante(comprobanteRepo);
        gestorCotizacion = new GestorCotizacion(pagos);
        reloj = Clock.fixed(INSTANTE, ZoneOffset.UTC);

        cita = new Cita();
        cita.setId(CITA_ID);
        cita.setClienteId(2L);
        cita.setTecnicoId(3L);
        cita.setEspecialidadId(5L);
        cita.setEstado(EstadoCita.FINALIZADA);
        when(pagoRepo.guardar(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
    }

    private Especialidad especialidad(String costo) {
        Especialidad e = new Especialidad(5L, "Lavadoras", "", true);
        e.setCostoReferencia(costo == null ? null : new BigDecimal(costo));
        return e;
    }

    private Pago pagoExistente() {
        Pago pago = Pago.nuevo(CITA_ID);
        pago.setId(1L);
        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.of(pago));
        when(pagoRepo.buscarPorCitaId(CITA_ID)).thenReturn(Optional.of(pago));
        return pago;
    }

    private Pago pagoConCostoFinal() {
        Pago pago = pagoExistente();
        pago.registrarCostoFinal(new BigDecimal("80000"));
        return pago;
    }

    private ConfiguracionPagos configuracion() {
        return new ConfiguracionPagos(BigDecimal.valueOf(20), "http://retorno");
    }

    // ---------- servicios de apoyo ----------

    @Test
    void obtenerBloqueandoSinPagoLanza404() {
        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.empty());

        assertThrows(PagoNoEncontradoException.class, () -> pagos.obtenerBloqueando(CITA_ID));
    }

    @Test
    void obtenerOCrearCreaElPagoPendienteSiNoExiste() {
        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.empty());

        Pago pago = pagos.obtenerOCrearBloqueando(CITA_ID);

        assertEquals(CITA_ID, pago.getCitaId());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void guardarAuditaSoloCuandoCambiaElEstadoYSinMontos() {
        Pago pago = Pago.nuevo(CITA_ID);

        pagos.guardar(pago, EstadoPago.PENDIENTE, CLIENTE);
        verifyNoInteractions(registroLog);

        pagos.guardar(pago, EstadoPago.FALLIDO, CLIENTE);
        verify(registroLog).registrar(any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("Pago de la cita 10: FALLIDO -> PENDIENTE"));
    }

    @Test
    void elEmisorSoloCreaUnComprobanteParaPagosPagadosYNuncaDos() {
        Pago pendiente = Pago.nuevo(CITA_ID);
        pendiente.setId(1L);
        emisor.emitirSiPagado(pendiente, AHORA);
        verifyNoInteractions(comprobanteRepo);

        Pago pagado = Pago.nuevo(CITA_ID);
        pagado.setId(1L);
        pagado.setEstado(EstadoPago.PAGADO);
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());
        emisor.emitirSiPagado(pagado, AHORA);
        verify(comprobanteRepo).guardar(new ComprobantePago(null, 1L, AHORA));

        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.of(new ComprobantePago(7L, 1L, AHORA)));
        emisor.emitirSiPagado(pagado, AHORA);
        verify(comprobanteRepo, org.mockito.Mockito.times(1)).guardar(any());
    }

    @Test
    void laCotizacionSeGuardaEnElPagoYSinCostoDeReferenciaNoCreaNada() {
        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.empty());

        Cotizacion con = gestorCotizacion.registrar(CITA_ID, especialidad("80000"));
        assertTrue(con.disponible());
        verify(pagoRepo, org.mockito.Mockito.atLeastOnce()).guardar(any(Pago.class));

        org.mockito.Mockito.clearInvocations(pagoRepo);
        Cotizacion sin = gestorCotizacion.registrar(CITA_ID, especialidad(null));
        assertTrue(!sin.disponible());
        verify(pagoRepo, never()).guardar(any());
    }

    // ---------- cotización ----------

    @Test
    void cotizarUnaEspecialidadActivaDevuelveElEstimado() {
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(especialidad("80000")));

        Cotizacion c = new ConsultarCotizacionEspecialidadUseCaseImpl(especialidadRepo).cotizar(5L);

        assertEquals(new BigDecimal("80000"), c.montoEstimado());
    }

    @Test
    void cotizarUnaEspecialidadInexistenteOInactivaFalla() {
        Especialidad inactiva = especialidad("80000");
        inactiva.setActivo(false);
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(inactiva));
        when(especialidadRepo.buscarPorId(6L)).thenReturn(Optional.empty());
        var useCase = new ConsultarCotizacionEspecialidadUseCaseImpl(especialidadRepo);

        assertThrows(EspecialidadNoEncontradaException.class, () -> useCase.cotizar(5L));
        assertThrows(EspecialidadNoEncontradaException.class, () -> useCase.cotizar(6L));
    }

    @Test
    void cotizarUnaCitaGuardaElMontoCotizadoEnElPago() {
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(especialidad("80000")));
        Pago pago = pagoExistente();

        Cotizacion c = new ConsultarCotizacionCitaUseCaseImpl(verificador, especialidadRepo, gestorCotizacion)
                .cotizar(CLIENTE, CITA_ID);

        assertEquals(new BigDecimal("80000"), c.montoEstimado());
        assertEquals(new BigDecimal("80000.00"), pago.getMontoCotizado());
    }

    @Test
    void cotizarUnaCitaDeEspecialidadSinCostoNoRompe() {
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(especialidad(null)));

        Cotizacion c = new ConsultarCotizacionCitaUseCaseImpl(verificador, especialidadRepo, gestorCotizacion)
                .cotizar(CLIENTE, CITA_ID);

        assertNull(c.montoEstimado());
        verify(pagoRepo, never()).guardar(any());
    }

    @Test
    void cotizarUnaCitaAjenaOConEspecialidadPerdidaFalla() {
        var useCase = new ConsultarCotizacionCitaUseCaseImpl(verificador, especialidadRepo, gestorCotizacion);
        when(verificador.exigirAcceso("otro@x.com", CITA_ID)).thenThrow(new AccesoCitaDenegadoException());
        assertThrows(AccesoCitaDenegadoException.class, () -> useCase.cotizar("otro@x.com", CITA_ID));

        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.empty());
        assertThrows(EspecialidadNoEncontradaException.class, () -> useCase.cotizar(CLIENTE, CITA_ID));
    }

    // ---------- costo final ----------

    @Test
    void elTecnicoRegistraElCostoFinalDeUnaCitaFinalizada() {
        when(verificador.exigirTecnicoAsignado(TECNICO, CITA_ID)).thenReturn(cita);
        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.empty());

        Pago pago = new RegistrarCostoFinalUseCaseImpl(verificador, pagos)
                .registrar(TECNICO, CITA_ID, new BigDecimal("95000"));

        assertEquals(new BigDecimal("95000.00"), pago.getMontoFinal());
    }

    @Test
    void elCostoFinalExigeCitaFinalizadaYMontoValido() {
        var useCase = new RegistrarCostoFinalUseCaseImpl(verificador, pagos);
        when(verificador.exigirTecnicoAsignado(TECNICO, CITA_ID)).thenReturn(cita);
        pagoExistente();

        assertThrows(DatosPagoInvalidosException.class, () -> useCase.registrar(TECNICO, CITA_ID, BigDecimal.ZERO));

        cita.setEstado(EstadoCita.EN_PROCESO);
        assertThrows(EstadoCitaInvalidoException.class, () -> useCase.registrar(TECNICO, CITA_ID, BigDecimal.TEN));
    }

    @Test
    void elCostoFinalDeOtroTecnicoEsRechazado() {
        when(verificador.exigirTecnicoAsignado("otro@x.com", CITA_ID)).thenThrow(new AccesoCitaDenegadoException());

        assertThrows(AccesoCitaDenegadoException.class,
                () -> new RegistrarCostoFinalUseCaseImpl(verificador, pagos).registrar("otro@x.com", CITA_ID, BigDecimal.TEN));
    }

    // ---------- método de pago ----------

    private ElegirMetodoPagoUseCaseImpl elegir() {
        return new ElegirMetodoPagoUseCaseImpl(verificador, pagos, pasarela, configuracion());
    }

    @Test
    void elegirEfectivoDejaElPagoPendienteSinTocarLaPasarela() {
        pagoConCostoFinal();

        ResultadoMetodoPago r = elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EFECTIVO);

        assertEquals(MetodoPago.EFECTIVO, r.pago().getMetodo());
        assertEquals(EstadoPago.PENDIENTE, r.pago().getEstado());
        assertNull(r.urlPago());
        verifyNoInteractions(pasarela);
    }

    @Test
    void elegirEnLineaIniciaUnIntentoConReferenciaNuevaYDevuelveLaUrl() {
        pagoConCostoFinal();
        when(pasarela.iniciarPago(any())).thenReturn(new InicioPago("APT-10-1-1", "http://pagar"));

        ResultadoMetodoPago r = elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA);

        assertEquals("http://pagar", r.urlPago());
        assertEquals("APT-10-1-1", r.pago().getReferenciaExterna());
        assertEquals(MetodoPago.EN_LINEA, r.pago().getMetodo());
        var captor = org.mockito.ArgumentCaptor.forClass(com.appointech.appointech_backend.domain.models.SolicitudPago.class);
        verify(pasarela).iniciarPago(captor.capture());
        assertEquals(8_000_000L, captor.getValue().montoEnCentavos());
        assertEquals("http://retorno?citaId=10", captor.getValue().urlRetorno());
        assertEquals(CLIENTE, captor.getValue().correoCliente());
    }

    @Test
    void siLaPasarelaFallaElPagoNoCambiaYSeInformaQueNoEstaDisponible() {
        Pago pago = pagoConCostoFinal();
        when(pasarela.iniciarPago(any())).thenThrow(new IllegalStateException("caida"));

        assertThrows(PasarelaNoDisponibleException.class, () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA));

        assertNull(pago.getMetodo());
        assertEquals(0, pago.getIntentosPago());
        verify(pagoRepo, never()).guardar(pago);
    }

    @Test
    void unaRespuestaSinUrlDeLaPasarelaSeTrataComoNoDisponible() {
        pagoConCostoFinal();
        when(pasarela.iniciarPago(any())).thenReturn(null).thenReturn(new InicioPago("r", " "));

        assertThrows(PasarelaNoDisponibleException.class, () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA));
        assertThrows(PasarelaNoDisponibleException.class, () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA));
    }

    @Test
    void elegirMetodoSinCostoFinalFalla() {
        pagoExistente();

        assertThrows(PagoInvalidoException.class, () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EFECTIVO));
        verifyNoInteractions(pasarela);
    }

    @Test
    void elegirMetodoDeUnaCitaAjenaFalla() {
        when(verificador.exigirDuenoCliente("otro@x.com", CITA_ID)).thenThrow(new AccesoCitaDenegadoException());

        assertThrows(AccesoCitaDenegadoException.class, () -> elegir().elegir("otro@x.com", CITA_ID, MetodoPago.EFECTIVO));
    }

    @Test
    void conUnPagoEnLineaPendienteElegirOtroMetodoOReiniciarSeRechazaSinTocarLaPasarela() {
        Pago pago = pagoConCostoFinal();
        pago.registrarIntentoEnLinea(pago.proximaReferencia());

        PagoInvalidoException ex = assertThrows(PagoInvalidoException.class,
                () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EFECTIVO));
        assertThrows(PagoInvalidoException.class, () -> elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA));

        assertEquals("Hay un pago en línea en proceso; verifica su resultado antes de cambiar de método.", ex.getMessage());
        verifyNoInteractions(pasarela);
        verify(pagoRepo, never()).guardar(pago);
    }

    @Test
    void conUnPagoEnLineaFallidoSePuedeReintentarConUnaReferenciaNueva() {
        Pago pago = pagoConCostoFinal();
        pago.registrarIntentoEnLinea(pago.proximaReferencia());
        pago.setEstado(EstadoPago.FALLIDO);
        when(pasarela.iniciarPago(any())).thenReturn(new InicioPago("APT-10-1-2", "http://pagar2"));

        ResultadoMetodoPago r = elegir().elegir(CLIENTE, CITA_ID, MetodoPago.EN_LINEA);

        assertEquals("APT-10-1-2", r.pago().getReferenciaExterna());
        assertEquals(EstadoPago.PENDIENTE, r.pago().getEstado());
        assertEquals("http://pagar2", r.urlPago());
    }

    // ---------- confirmar efectivo ----------

    private ConfirmarPagoEfectivoUseCaseImpl confirmar() {
        return new ConfirmarPagoEfectivoUseCaseImpl(verificador, pagos, emisor, reloj);
    }

    @Test
    void confirmarEfectivoPagaYGeneraElComprobanteAutomaticamente() {
        Pago pago = pagoConCostoFinal();
        pago.usarEfectivo();
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());

        Pago r = confirmar().confirmar(TECNICO, CITA_ID);

        assertEquals(EstadoPago.PAGADO, r.getEstado());
        assertEquals(AHORA, r.getPagadoEn());
        verify(comprobanteRepo).guardar(new ComprobantePago(null, 1L, AHORA));
    }

    @Test
    void confirmarEfectivoDosVecesFallaYNoCreaOtroComprobante() {
        Pago pago = pagoConCostoFinal();
        pago.usarEfectivo();
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());
        confirmar().confirmar(TECNICO, CITA_ID);

        assertThrows(PagoInvalidoException.class, () -> confirmar().confirmar(TECNICO, CITA_ID));
        verify(comprobanteRepo, org.mockito.Mockito.times(1)).guardar(any());
    }

    @Test
    void confirmarEfectivoSinHaberElegidoEfectivoOSinPagoFalla() {
        pagoConCostoFinal();
        assertThrows(PagoInvalidoException.class, () -> confirmar().confirmar(TECNICO, CITA_ID));

        when(pagoRepo.buscarPorCitaIdBloqueando(CITA_ID)).thenReturn(Optional.empty());
        assertThrows(PagoNoEncontradoException.class, () -> confirmar().confirmar(TECNICO, CITA_ID));
        verifyNoInteractions(comprobanteRepo);
    }

    // ---------- sincronizar ----------

    private SincronizarPagoUseCaseImpl sincronizar() {
        return new SincronizarPagoUseCaseImpl(verificador, pagos, pasarela, emisor, reloj);
    }

    private Pago pagoEnLinea() {
        Pago pago = pagoConCostoFinal();
        pago.registrarIntentoEnLinea(pago.proximaReferencia());
        return pago;
    }

    private void transaccion(String referencia, long centavos, EstadoTransaccionPasarela estado) {
        when(pasarela.consultarTransaccion("tx-1")).thenReturn(new ResultadoTransaccion("tx-1", referencia, centavos, estado));
    }

    @Test
    void unaTransaccionAprobadaPagaYGeneraElComprobante() {
        pagoEnLinea();
        transaccion("APT-10-1-1", 8_000_000, EstadoTransaccionPasarela.APROBADA);
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());

        Pago r = sincronizar().sincronizar(CLIENTE, CITA_ID, " tx-1 ");

        assertEquals(EstadoPago.PAGADO, r.getEstado());
        assertEquals("tx-1", r.getIdTransaccionExterna());
        verify(comprobanteRepo).guardar(new ComprobantePago(null, 1L, AHORA));
    }

    @Test
    void unaTransaccionRechazadaDejaElPagoFallidoSinComprobante() {
        pagoEnLinea();
        transaccion("APT-10-1-1", 8_000_000, EstadoTransaccionPasarela.RECHAZADA);

        Pago r = sincronizar().sincronizar(CLIENTE, CITA_ID, "tx-1");

        assertEquals(EstadoPago.FALLIDO, r.getEstado());
        verifyNoInteractions(comprobanteRepo);
    }

    @Test
    void sincronizarUnPagoYaPagadoNoConsultaLaPasarelaNiDuplicaElComprobante() {
        Pago pago = pagoEnLinea();
        pago.setEstado(EstadoPago.PAGADO);

        Pago r = sincronizar().sincronizar(CLIENTE, CITA_ID, "tx-1");

        assertSame(pago, r);
        verifyNoInteractions(pasarela);
        verifyNoInteractions(comprobanteRepo);
    }

    @Test
    void unaTransaccionConOtraReferenciaOMontoNoActualizaElPago() {
        Pago pago = pagoEnLinea();
        var useCase = sincronizar();

        transaccion("APT-99-9-1", 8_000_000, EstadoTransaccionPasarela.APROBADA);
        assertThrows(TransaccionNoCoincideException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, "tx-1"));
        transaccion("APT-10-1-1", 1, EstadoTransaccionPasarela.APROBADA);
        assertThrows(TransaccionNoCoincideException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, "tx-1"));

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        verify(pagoRepo, never()).guardar(pago);
    }

    @Test
    void sincronizarSinIdDeTransaccionConsultaPorLaReferenciaGuardadaYPaga() {
        pagoEnLinea();
        when(pasarela.consultarPorReferencia("APT-10-1-1", 8_000_000L))
                .thenReturn(new ResultadoTransaccion("SIM:APT-10-1-1:8000000", "APT-10-1-1", 8_000_000L,
                        EstadoTransaccionPasarela.APROBADA));
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());

        for (String sinId : new String[]{null, " "}) {
            Pago r = sincronizar().sincronizar(CLIENTE, CITA_ID, sinId);
            assertEquals(EstadoPago.PAGADO, r.getEstado());
        }

        verify(pasarela, never()).consultarTransaccion(any());
        verify(comprobanteRepo).guardar(new ComprobantePago(null, 1L, AHORA));
    }

    @Test
    void sincronizarPorReferenciaValidaElMontoYUnPagadoNoCambia() {
        Pago pago = pagoEnLinea();
        when(pasarela.consultarPorReferencia("APT-10-1-1", 8_000_000L))
                .thenReturn(new ResultadoTransaccion("x", "APT-10-1-1", 1L, EstadoTransaccionPasarela.APROBADA));
        var useCase = sincronizar();

        assertThrows(TransaccionNoCoincideException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, null));
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());

        pago.setEstado(EstadoPago.PAGADO);
        assertSame(pago, useCase.sincronizar(CLIENTE, CITA_ID, null));
    }

    @Test
    void sincronizarSinIdDeTransaccionSiElPagoNoEsEnLineaOSinReferenciaResponde409() {
        var useCase = sincronizar();
        Pago pago = pagoConCostoFinal();
        pago.usarEfectivo();

        assertThrows(PagoInvalidoException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, null));

        pago.setMetodo(MetodoPago.EN_LINEA);
        pago.setReferenciaExterna(null);
        assertThrows(PagoInvalidoException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, " "));
        verifyNoInteractions(pasarela);
    }

    @Test
    void sincronizarConIdDeTransaccionSiElPagoNoEsEnLineaTambienResponde409() {
        Pago pago = pagoConCostoFinal();
        pago.usarEfectivo();
        transaccion("APT-10-1-1", 8_000_000, EstadoTransaccionPasarela.APROBADA);

        assertThrows(PagoInvalidoException.class, () -> sincronizar().sincronizar(CLIENTE, CITA_ID, "tx-1"));
    }

    @Test
    void siLaPasarelaFallaAlConsultarPorReferenciaSeInformaQueNoEstaDisponible() {
        pagoEnLinea();
        when(pasarela.consultarPorReferencia(any(), org.mockito.ArgumentMatchers.anyLong()))
                .thenThrow(new IllegalStateException("timeout"));

        assertThrows(PasarelaNoDisponibleException.class, () -> sincronizar().sincronizar(CLIENTE, CITA_ID, null));
    }

    @Test
    void siLaPasarelaFallaAlConsultarSeInformaQueNoEstaDisponible() {
        pagoEnLinea();
        var useCase = sincronizar();

        when(pasarela.consultarTransaccion("tx-1")).thenThrow(new IllegalStateException("timeout"));
        assertThrows(PasarelaNoDisponibleException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, "tx-1"));

        org.mockito.Mockito.doReturn(null).when(pasarela).consultarTransaccion("tx-1");
        assertThrows(PasarelaNoDisponibleException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, "tx-1"));

        org.mockito.Mockito.doReturn(new ResultadoTransaccion("tx-1", "r", 1, null)).when(pasarela).consultarTransaccion("tx-1");
        assertThrows(PasarelaNoDisponibleException.class, () -> useCase.sincronizar(CLIENTE, CITA_ID, "tx-1"));
    }

    // ---------- consultar pago y comprobante ----------

    @Test
    void consultarElPagoDevuelveElDeLaCitaOLanza404() {
        var useCase = new ConsultarPagoUseCaseImpl(verificador, pagoRepo);
        Pago pago = pagoExistente();
        assertSame(pago, useCase.consultar(CLIENTE, CITA_ID));

        when(pagoRepo.buscarPorCitaId(CITA_ID)).thenReturn(Optional.empty());
        assertThrows(PagoNoEncontradoException.class, () -> useCase.consultar(CLIENTE, CITA_ID));
    }

    private ConsultarComprobanteUseCaseImpl comprobante() {
        return new ConsultarComprobanteUseCaseImpl(verificador, pagoRepo, comprobanteRepo, clienteRepo, tecnicoRepo,
                especialidadRepo);
    }

    private Usuario usuario(String nombre, String apellido) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        return u;
    }

    @Test
    void elComprobanteSeArmaConLosNombresDeClienteTecnicoYEspecialidad() {
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        Pago pago = pagoConCostoFinal();
        pago.setEstado(EstadoPago.PAGADO);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setPagadoEn(AHORA);
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.of(new ComprobantePago(1L, 1L, AHORA)));
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario("Ana", "Pérez"));
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente));
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario("Luis", "Gómez"));
        when(tecnicoRepo.buscarPorId(3L)).thenReturn(Optional.of(tecnico));
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(especialidad("80000")));

        ComprobanteDetalle d = comprobante().consultar(CLIENTE, CITA_ID);

        assertEquals("COMP-000001", d.numero());
        assertEquals("Ana Pérez", d.cliente());
        assertEquals("Luis Gómez", d.tecnico());
        assertEquals("Lavadoras", d.especialidad());
        assertEquals(new BigDecimal("80000.00"), d.monto());
        assertEquals(MetodoPago.EFECTIVO, d.metodo());
    }

    @Test
    void elComprobanteToleraTecnicoYDatosAusentes() {
        cita.setTecnicoId(null);
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        Pago pago = pagoExistente();
        pago.setEstado(EstadoPago.PAGADO);
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.of(new ComprobantePago(2L, 1L, AHORA)));
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.empty());
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.empty());

        ComprobanteDetalle d = comprobante().consultar(CLIENTE, CITA_ID);

        assertNull(d.tecnico());
        assertNull(d.cliente());
        assertNull(d.especialidad());
    }

    @Test
    void sinPagoPagadoNoHayComprobanteNiSeCreaUno() {
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        var useCase = comprobante();

        when(pagoRepo.buscarPorCitaId(CITA_ID)).thenReturn(Optional.empty());
        assertThrows(PagoNoEncontradoException.class, () -> useCase.consultar(CLIENTE, CITA_ID));

        pagoExistente();
        ComprobanteNoDisponibleException ex = assertThrows(ComprobanteNoDisponibleException.class,
                () -> useCase.consultar(CLIENTE, CITA_ID));
        assertTrue(ex.getMessage().contains("PENDIENTE"));
        verify(comprobanteRepo, never()).guardar(any());
    }

    @Test
    void unPagoPagadoSinComprobanteResponde409SinCrearlo() {
        when(verificador.exigirAcceso(CLIENTE, CITA_ID)).thenReturn(cita);
        pagoExistente().setEstado(EstadoPago.PAGADO);
        when(comprobanteRepo.buscarPorPagoId(1L)).thenReturn(Optional.empty());

        assertThrows(ComprobanteNoDisponibleException.class, () -> comprobante().consultar(CLIENTE, CITA_ID));
        verify(comprobanteRepo, never()).guardar(any());
    }
}
