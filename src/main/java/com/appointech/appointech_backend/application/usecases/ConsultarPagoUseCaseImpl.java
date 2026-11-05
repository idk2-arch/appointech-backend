package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: consultar el pago de una cita (cliente dueño, técnico asignado o administrador).
 * <p>Historias de usuario: SCRUM-24 (RF22 - costo final), SCRUM-25 (RF23 - pago).
 */
@Service
public class ConsultarPagoUseCaseImpl implements ConsultarPagoUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagoRepositoryPort pagoRepositoryPort;

    public ConsultarPagoUseCaseImpl(VerificadorAccesoCita verificadorAcceso, PagoRepositoryPort pagoRepositoryPort) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    public Pago consultar(String correo, Long citaId) {
        verificadorAcceso.exigirAcceso(correo, citaId);
        return pagoRepositoryPort.buscarPorCitaId(citaId).orElseThrow(() -> new PagoNoEncontradoException(citaId));
    }
}
