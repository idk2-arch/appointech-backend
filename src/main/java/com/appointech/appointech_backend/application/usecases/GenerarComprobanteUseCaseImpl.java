package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.CitaAccesoService;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.ports.in.GenerarComprobanteUseCase;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GenerarComprobanteUseCaseImpl implements GenerarComprobanteUseCase {

    private final CitaAccesoService accesoService;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final ComprobantePagoRepositoryPort comprobanteRepositoryPort;

    public GenerarComprobanteUseCaseImpl(CitaAccesoService accesoService,
                                         PagoRepositoryPort pagoRepositoryPort,
                                         ComprobantePagoRepositoryPort comprobanteRepositoryPort) {
        this.accesoService = accesoService;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.comprobanteRepositoryPort = comprobanteRepositoryPort;
    }

    @Override
    @Transactional
    public ComprobantePago generar(Long citaId, String correoActor) {
        validarAcceso(citaId, correoActor);

        Optional<ComprobantePago> existente = comprobanteRepositoryPort.buscarPorCitaId(citaId);
        if (existente.isPresent()) {
            return existente.get();
        }

        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new ComprobanteNoDisponibleException("La cita no tiene un pago registrado"));
        return comprobanteRepositoryPort.guardar(ComprobantePago.emitir(pago));
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobantePago consultar(Long citaId, String correoActor) {
        validarAcceso(citaId, correoActor);

        return comprobanteRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new ComprobanteNoDisponibleException("La cita aún no tiene comprobante"));
    }

    private void validarAcceso(Long citaId, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        accesoService.exigirParticipanteOAdmin(cita, actor);
    }
}
