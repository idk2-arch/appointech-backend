package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.CitaAccesoService;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.CotizacionNoDisponibleException;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionUseCase;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ConsultarCotizacionUseCaseImpl implements ConsultarCotizacionUseCase {

    private final CitaAccesoService accesoService;
    private final PagoRepositoryPort pagoRepositoryPort;

    public ConsultarCotizacionUseCaseImpl(CitaAccesoService accesoService, PagoRepositoryPort pagoRepositoryPort) {
        this.accesoService = accesoService;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    @Transactional
    public Cotizacion consultar(Long citaId, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        accesoService.exigirParticipanteOAdmin(cita, actor);

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new CotizacionNoDisponibleException("No hay cotización para una cita cancelada");
        }

        String nombreEspecialidad = cita.getEspecialidad().getNombre();
        BigDecimal costoReferencia = cita.getEspecialidad().getCostoReferencia();
        if (costoReferencia == null) {
            throw new CotizacionNoDisponibleException(
                    "La especialidad '" + nombreEspecialidad + "' no tiene un costo de referencia configurado");
        }

        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId).orElseGet(() -> Pago.nuevo(cita));
        if (pago.registrarCotizacion(costoReferencia)) {
            pago = pagoRepositoryPort.guardar(pago);
        }
        return new Cotizacion(cita.getId(), nombreEspecialidad, pago.getMontoCotizado());
    }
}
