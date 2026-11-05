package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EmisorComprobante;
import com.appointech.appointech_backend.application.services.PagosDeCita;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.PasarelaNoDisponibleException;
import com.appointech.appointech_backend.domain.models.Dinero;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.ports.in.SincronizarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: actualiza el pago en línea con lo que dice la pasarela. El id de transacción es opcional: sin él se consulta por la
 * referencia guardada en el pago. En ambos casos el pago se actualiza únicamente si la referencia y el monto de la
 * transacción son los suyos.
 * <p>Historias de usuario: SCRUM-25 (RF23 - pago en línea), SCRUM-26 (RF24 - comprobante).
 */
@Service
public class SincronizarPagoUseCaseImpl implements SincronizarPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarPagoUseCaseImpl.class);

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagosDeCita pagos;
    private final PasarelaPagoPort pasarela;
    private final EmisorComprobante emisorComprobante;
    private final Clock reloj;

    public SincronizarPagoUseCaseImpl(VerificadorAccesoCita verificadorAcceso, PagosDeCita pagos,
                                      PasarelaPagoPort pasarela, EmisorComprobante emisorComprobante, Clock reloj) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.emisorComprobante = emisorComprobante;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Pago sincronizar(String correo, Long citaId, String idTransaccion) {
        verificadorAcceso.exigirAcceso(correo, citaId);
        Pago pago = pagos.obtenerBloqueando(citaId);
        if (pago.getEstado() == EstadoPago.PAGADO) {
            return pago;
        }
        EstadoPago anterior = pago.getEstado();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        pago.aplicarResultado(consultarPasarela(pago, idTransaccion), ahora);
        Pago guardado = pagos.guardar(pago, anterior, correo);
        emisorComprobante.emitirSiPagado(guardado, ahora);
        return guardado;
    }

    private ResultadoTransaccion consultarPasarela(Pago pago, String idTransaccion) {
        Long citaId = pago.getCitaId();
        boolean porReferencia = idTransaccion == null || idTransaccion.isBlank();
        if (porReferencia) {
            pago.exigirPagoEnLineaIniciado();
        }
        ResultadoTransaccion resultado;
        try {
            resultado = porReferencia
                    ? pasarela.consultarPorReferencia(pago.getReferenciaExterna(), Dinero.aCentavos(pago.getMontoFinal()))
                    : pasarela.consultarTransaccion(idTransaccion.strip());
        } catch (RuntimeException ex) {
            log.warn("La pasarela no pudo consultar la transacción de la cita {}: {}", citaId,
                    ex.getClass().getSimpleName());
            throw new PasarelaNoDisponibleException();
        }
        if (resultado == null || resultado.estado() == null) {
            throw new PasarelaNoDisponibleException();
        }
        return resultado;
    }
}
