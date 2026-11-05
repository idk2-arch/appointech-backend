package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelosDePagoTest {

    @Test
    void dineroSeRedondeaADosDecimalesYSeConvierteACentavos() {
        assertEquals(new BigDecimal("80000.00"), Dinero.redondear(new BigDecimal("80000")));
        assertEquals(new BigDecimal("0.13"), Dinero.redondear(new BigDecimal("0.125")));
        assertEquals(8_000_000L, Dinero.aCentavos(new BigDecimal("80000")));
        assertEquals(12_345L, Dinero.aCentavos(new BigDecimal("123.45")));
        assertEquals(1L, Dinero.aCentavos(new BigDecimal("0.005")));
    }

    @Test
    void laSolicitudDeCobroLlevaElMontoEnCentavosYLaMonedaCop() {
        SolicitudPago solicitud = SolicitudPago.cobro("APT-1-1-1", new BigDecimal("80000"), "c@x.com", "http://r");

        assertEquals("APT-1-1-1", solicitud.referencia());
        assertEquals(8_000_000L, solicitud.montoEnCentavos());
        assertEquals("COP", solicitud.moneda());
        assertEquals("c@x.com", solicitud.correoCliente());
        assertEquals("http://r", solicitud.urlRetorno());
    }

    @Test
    void laCotizacionConCostoDeReferenciaEstaDisponibleYExplicaQueEsEstimada() {
        Especialidad lavadoras = new Especialidad(2L, "Lavadoras", "", true);
        lavadoras.setCostoReferencia(new BigDecimal("80000"));

        Cotizacion cotizacion = Cotizacion.de(lavadoras);

        assertTrue(cotizacion.disponible());
        assertEquals(2L, cotizacion.especialidadId());
        assertEquals("Lavadoras", cotizacion.especialidad());
        assertEquals(new BigDecimal("80000"), cotizacion.montoEstimado());
        assertEquals("Valor estimado; el costo final lo registra el técnico", cotizacion.nota());
    }

    @Test
    void laCotizacionSinCostoDeReferenciaNoRompeYDaUnMensajeClaro() {
        Cotizacion cotizacion = Cotizacion.de(new Especialidad(3L, "Estufas", "", true));

        assertFalse(cotizacion.disponible());
        assertNull(cotizacion.montoEstimado());
        assertTrue(cotizacion.nota().contains("aún no tiene un valor de referencia"));
    }

    @Test
    void elComprobanteTieneNumeroConSeisDigitos() {
        ComprobantePago comprobante = new ComprobantePago(1L, 5L, LocalDateTime.now());

        assertEquals("COMP-000001", comprobante.numero());
        assertEquals("COMP-123456", new ComprobantePago(123456L, 5L, LocalDateTime.now()).numero());
    }

    @Test
    void laConfiguracionDePagosRechazaUnUmbralNegativoONulo() {
        assertEquals(new BigDecimal("20"), new ConfiguracionPagos(new BigDecimal("20"), "http://r").umbralDiferenciaPorcentaje());
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionPagos(new BigDecimal("-1"), "http://r"));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionPagos(null, "http://r"));
    }

    @Test
    void laEspecialidadGuardaSuCostoDeReferencia() {
        Especialidad especialidad = new Especialidad();
        assertNull(especialidad.getCostoReferencia());

        especialidad.setCostoReferencia(new BigDecimal("70000"));

        assertEquals(new BigDecimal("70000"), especialidad.getCostoReferencia());
    }

    @Test
    void soloUnaCitaFinalizadaPasaLaValidacionDeFinalizada() {
        Cita finalizada = new Cita();
        finalizada.setEstado(EstadoCita.FINALIZADA);
        finalizada.exigirFinalizada("registrar el costo final");

        Cita enProceso = new Cita();
        enProceso.setEstado(EstadoCita.EN_PROCESO);
        EstadoCitaInvalidoException ex = assertThrows(EstadoCitaInvalidoException.class,
                () -> enProceso.exigirFinalizada("registrar el costo final"));
        assertEquals("Solo se puede registrar el costo final cuando la cita está FINALIZADA; la cita está EN_PROCESO",
                ex.getMessage());
    }

    @Test
    void laUrlDeRetornoAgregaElCitaIdRespetandoParametrosYFragmento() {
        assertEquals("http://r/pago?citaId=7", url("http://r/pago").urlRetornoPara(7L));
        assertEquals("http://r/pago?lang=es&citaId=7", url("http://r/pago?lang=es").urlRetornoPara(7L));
        assertEquals("http://r/pago?citaId=7", url("http://r/pago?").urlRetornoPara(7L));
        assertEquals("http://r/pago?a=1&citaId=7", url("http://r/pago?a=1&").urlRetornoPara(7L));
        assertEquals("http://r/pago?citaId=7#res", url("http://r/pago#res").urlRetornoPara(7L));
    }

    private static ConfiguracionPagos url(String base) {
        return new ConfiguracionPagos(BigDecimal.TEN, base);
    }
}
