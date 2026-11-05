package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.CitaAccesoService;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPagoUseCaseImpl implements ConsultarPagoUseCase {

    private final CitaAccesoService accesoService;
    private final PagoRepositoryPort pagoRepositoryPort;

    public ConsultarPagoUseCaseImpl(CitaAccesoService accesoService, PagoRepositoryPort pagoRepositoryPort) {
        this.accesoService = accesoService;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Pago consultar(Long citaId, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        accesoService.exigirParticipanteOAdmin(cita, actor);

        return pagoRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new PagoNoEncontradoException(citaId));
    }
}
