package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.models.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.models.exception.CostoFinalNoPermitidoException;
import com.appointech.appointech_backend.domain.models.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.models.exception.PagoNoPermitidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoTest {

    private Pago pagoConCitaFinalizada() {
        Cita cita = new Cita();
        cita.setId(1L);
        cita.setEstado(EstadoCita.FINALIZADA);
        return Pago.nuevo(cita);
    }

    @Test
    void costoFinalSoloConCitaFinalizada() {
        Cita cita = new Cita();
        cita.setId(1L);
        cita.setEstado(EstadoCita.EN_PROCESO);
        Pago pago = Pago.nuevo(cita);

        assertThrows(CostoFinalNoPermitidoException.class, () -> pago.registrarCostoFinal(new BigDecimal("50000")));
    }

    @Test
    void costoFinalDebeSerMayorQueCero() {
        Pago pago = pagoConCitaFinalizada();

        assertThrows(DatosPagoInvalidosException.class, () -> pago.registrarCostoFinal(BigDecimal.ZERO));
    }

    @Test
    void metodoDePagoExigeCostoFinal() {
        Pago pago = pagoConCitaFinalizada();

        assertThrows(PagoNoPermitidoException.class, () -> pago.seleccionarMetodo(MetodoPago.EFECTIVO));
    }

    @Test
    void pagoFallidoPermiteReintentarConOtroMetodo() {
        Pago pago = pagoConCitaFinalizada();
        pago.registrarCostoFinal(new BigDecimal("90000"));
        pago.seleccionarMetodo(MetodoPago.EN_LINEA);
        pago.registrarResultadoEnLinea(false, "REF-1");
        assertEquals(EstadoPago.FALLIDO, pago.getEstado());

        pago.seleccionarMetodo(MetodoPago.EFECTIVO);
        pago.confirmarEfectivo();

        assertTrue(pago.estaPagado());
    }

    @Test
    void pagoConfirmadoNoPermiteCambios() {
        Pago pago = pagoConCitaFinalizada();
        pago.registrarCostoFinal(new BigDecimal("90000"));
        pago.seleccionarMetodo(MetodoPago.EFECTIVO);
        pago.confirmarEfectivo();

        assertThrows(PagoNoPermitidoException.class, () -> pago.registrarCostoFinal(new BigDecimal("10000")));
        assertThrows(PagoNoPermitidoException.class, () -> pago.seleccionarMetodo(MetodoPago.EN_LINEA));
    }

    @Test
    void comprobanteSoloConPagoConfirmado() {
        Pago pago = pagoConCitaFinalizada();
        pago.registrarCostoFinal(new BigDecimal("90000"));

        assertThrows(ComprobanteNoDisponibleException.class, () -> ComprobantePago.emitir(pago));
    }
}
