package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.CitaAccesoService;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.RegistrarCostoFinalUseCase;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RegistrarCostoFinalUseCaseImpl implements RegistrarCostoFinalUseCase {

    private final CitaAccesoService accesoService;
    private final PagoRepositoryPort pagoRepositoryPort;

    public RegistrarCostoFinalUseCaseImpl(CitaAccesoService accesoService, PagoRepositoryPort pagoRepositoryPort) {
        this.accesoService = accesoService;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    @Transactional
    public Pago registrar(Long citaId, BigDecimal montoFinal, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        accesoService.exigirTecnicoAsignado(cita, actor);

        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId).orElseGet(() -> Pago.nuevo(cita));
        pago.registrarCostoFinal(montoFinal);
        return pagoRepositoryPort.guardar(pago);
    }
}
