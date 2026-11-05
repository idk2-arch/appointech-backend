package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.PagosDeCita;
import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.in.RegistrarCostoFinalUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Caso de uso: el técnico asignado registra el costo final de una cita FINALIZADA.
 * <p>Historias de usuario: SCRUM-24 (RF22 - costo final).
 */
@Service
public class RegistrarCostoFinalUseCaseImpl implements RegistrarCostoFinalUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagosDeCita pagos;

    public RegistrarCostoFinalUseCaseImpl(VerificadorAccesoCita verificadorAcceso, PagosDeCita pagos) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagos = pagos;
    }

    @Override
    @Transactional
    public Pago registrar(String correoTecnico, Long citaId, BigDecimal monto) {
        Cita cita = verificadorAcceso.exigirTecnicoAsignado(correoTecnico, citaId);
        cita.exigirFinalizada("registrar el costo final");

        Pago pago = pagos.obtenerOCrearBloqueando(citaId);
        EstadoPago anterior = pago.getEstado();
        pago.registrarCostoFinal(monto);
        return pagos.guardar(pago, anterior, correoTecnico);
    }
}
