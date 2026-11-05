package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.PagosDeCita;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoMetodoPago;
import com.appointech.appointech_backend.domain.models.SolicitudPago;
import com.appointech.appointech_backend.domain.ports.in.ElegirMetodoPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el cliente elige cómo pagar. En efectivo queda PENDIENTE hasta que el técnico confirme; en línea se
 * inicia un intento nuevo en la pasarela. Si la pasarela falla, el pago no cambia y el efectivo sigue disponible.
 * <p>Historias de usuario: SCRUM-25 (RF23 - método de pago).
 */
@Service
public class ElegirMetodoPagoUseCaseImpl implements ElegirMetodoPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ElegirMetodoPagoUseCaseImpl.class);

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagosDeCita pagos;
    private final PasarelaPagoPort pasarela;
    private final ConfiguracionPagos configuracion;

    public ElegirMetodoPagoUseCaseImpl(VerificadorAccesoCita verificadorAcceso, PagosDeCita pagos,
                                       PasarelaPagoPort pasarela, ConfiguracionPagos configuracion) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.configuracion = configuracion;
    }

    @Override
    @Transactional
    public ResultadoMetodoPago elegir(String correoCliente, Long citaId, MetodoPago metodo) {
        verificadorAcceso.exigirDuenoCliente(correoCliente, citaId);
        Pago pago = pagos.obtenerBloqueando(citaId);
        pago.validarPuedeCobrarse();
        EstadoPago anterior = pago.getEstado();

        if (metodo == MetodoPago.EFECTIVO) {
            pago.usarEfectivo();
            return new ResultadoMetodoPago(pagos.guardar(pago, anterior, correoCliente), null);
        }
        String urlPago = iniciarEnLinea(pago, correoCliente);
        return new ResultadoMetodoPago(pagos.guardar(pago, anterior, correoCliente), urlPago);
    }

    private String iniciarEnLinea(Pago pago, String correoCliente) {
        String referencia = pago.proximaReferencia();
        InicioPago inicio;
        try {
            inicio = pasarela.iniciarPago(
                    SolicitudPago.cobro(referencia, pago.getMontoFinal(), correoCliente, configuracion.urlRetornoPara(pago.getCitaId())));
        } catch (RuntimeException ex) {
            log.warn("La pasarela no pudo iniciar el pago de la cita {}: {}", pago.getCitaId(),
                    ex.getClass().getSimpleName());
            throw new PasarelaNoDisponibleException();
        }
        if (inicio == null || inicio.urlPago() == null || inicio.urlPago().isBlank()) {
            log.warn("La pasarela respondió sin URL de pago para la cita {}", pago.getCitaId());
            throw new PasarelaNoDisponibleException();
        }
        pago.registrarIntentoEnLinea(referencia);
        return inicio.urlPago();
    }
}
