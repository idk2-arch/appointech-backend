package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.CitaAccesoService;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.ResultadoPasarelaPago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.exception.AccesoCitaDenegadoException;
import com.appointech.appointech_backend.domain.models.exception.PagoNoPermitidoException;
import com.appointech.appointech_backend.domain.ports.in.SeleccionarMetodoPagoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeleccionarMetodoPagoUseCaseImpl implements SeleccionarMetodoPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SeleccionarMetodoPagoUseCaseImpl.class);

    private final CitaAccesoService accesoService;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final PasarelaPagoPort pasarelaPagoPort;

    public SeleccionarMetodoPagoUseCaseImpl(CitaAccesoService accesoService,
                                            PagoRepositoryPort pagoRepositoryPort,
                                            PasarelaPagoPort pasarelaPagoPort) {
        this.accesoService = accesoService;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.pasarelaPagoPort = pasarelaPagoPort;
    }

    @Override
    @Transactional
    public Pago seleccionar(Long citaId, MetodoPago metodo, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        if (!accesoService.esClienteDeLaCita(cita, actor)) {
            throw new AccesoCitaDenegadoException("Solo el cliente de la cita puede seleccionar el método de pago");
        }

        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new PagoNoPermitidoException(
                        "El pago exige que el técnico registre primero el costo final"));
        pago.seleccionarMetodo(metodo);

        if (metodo == MetodoPago.EN_LINEA) {
            ResultadoPasarelaPago resultado = procesarEnPasarela(pago, actor);
            pago.registrarResultadoEnLinea(resultado.aprobado(), resultado.referencia());
        }
        return pagoRepositoryPort.guardar(pago);
    }

    @Override
    @Transactional
    public Pago confirmarEfectivo(Long citaId, String correoActor) {
        Usuario actor = accesoService.cargarActor(correoActor);
        Cita cita = accesoService.cargarCita(citaId);
        accesoService.exigirTecnicoAsignado(cita, actor);

        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new PagoNoPermitidoException("La cita no tiene un pago en efectivo pendiente"));
        pago.confirmarEfectivo();
        return pagoRepositoryPort.guardar(pago);
    }

    private ResultadoPasarelaPago procesarEnPasarela(Pago pago, Usuario cliente) {
        try {
            return pasarelaPagoPort.procesarPago(pago.getCita().getId(), pago.getMontoFinal(), cliente.getCorreo());
        } catch (RuntimeException e) {
            log.warn("La pasarela falló al procesar el pago de la cita {}: {}", pago.getCita().getId(), e.getMessage());
            return new ResultadoPasarelaPago(false, null);
        }
    }
}
