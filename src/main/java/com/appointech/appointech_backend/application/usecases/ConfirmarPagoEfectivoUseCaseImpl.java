package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.EmisorComprobante;
import com.appointech.appointech_backend.application.services.PagosDeCita;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarPagoEfectivoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el técnico asignado confirma que recibió el efectivo; el pago queda PAGADO y se genera el comprobante.
 * <p>Historias de usuario: SCRUM-25 (RF23 - pago en efectivo), SCRUM-26 (RF24 - comprobante).
 */
@Service
public class ConfirmarPagoEfectivoUseCaseImpl implements ConfirmarPagoEfectivoUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagosDeCita pagos;
    private final EmisorComprobante emisorComprobante;
    private final Clock reloj;

    public ConfirmarPagoEfectivoUseCaseImpl(VerificadorAccesoCita verificadorAcceso, PagosDeCita pagos,
                                            EmisorComprobante emisorComprobante, Clock reloj) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagos = pagos;
        this.emisorComprobante = emisorComprobante;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Pago confirmar(String correoTecnico, Long citaId) {
        verificadorAcceso.exigirTecnicoAsignado(correoTecnico, citaId);
        Pago pago = pagos.obtenerBloqueando(citaId);
        EstadoPago anterior = pago.getEstado();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        pago.confirmarEfectivo(ahora);
        Pago guardado = pagos.guardar(pago, anterior, correoTecnico);
        emisorComprobante.emitirSiPagado(guardado, ahora);
        return guardado;
    }
}
