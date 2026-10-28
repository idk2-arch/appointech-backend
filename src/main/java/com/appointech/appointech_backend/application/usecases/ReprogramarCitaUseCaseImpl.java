package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ReprogramarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Caso de uso: el cliente mueve su cita a otra franja del mismo técnico. La franja se valida con el cálculo de
 * disponibilidad existente y la anterior queda libre al cambiar las horas de la cita.
 * <p>Historias de usuario: SCRUM-21 (reprogramar), SCRUM-20 (notificar).
 */
@Service
public class ReprogramarCitaUseCaseImpl implements ReprogramarCitaUseCase {

    private final ResolverClienteAutenticado resolverCliente;
    private final CitaRepositoryPort citaRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final CalcularFranjasDisponiblesUseCase calcularFranjas;
    private final GestorCambiosCita gestor;

    public ReprogramarCitaUseCaseImpl(ResolverClienteAutenticado resolverCliente,
                                      CitaRepositoryPort citaRepositoryPort,
                                      TecnicoRepositoryPort tecnicoRepositoryPort,
                                      CalcularFranjasDisponiblesUseCase calcularFranjas,
                                      GestorCambiosCita gestor) {
        this.resolverCliente = resolverCliente;
        this.citaRepositoryPort = citaRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.calcularFranjas = calcularFranjas;
        this.gestor = gestor;
    }

    @Override
    @Transactional
    public Cita reprogramar(String correoCliente, Long citaId, LocalDateTime nuevoInicio) {
        Cliente cliente = resolverCliente.resolver(correoCliente);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> cliente.getId().equals(c.getClienteId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));

        cita.validarModificablePorCliente("reprogramar");
        Long tecnicoId = cita.getTecnicoId();
        if (tecnicoId == null) {
            throw new FranjaNoDisponibleException(
                    "La solicitud aún no tiene técnico asignado: confirma una franja disponible en lugar de reprogramar");
        }
        if (!nuevoInicio.isAfter(gestor.ahora())) {
            throw new FranjaInvalidaException("La franja debe estar en el futuro");
        }
        if (cita.getLatitud() == null || cita.getLongitud() == null) {
            throw new FranjaNoDisponibleException("La cita no tiene coordenadas para validar la franja");
        }

        // Se bloquea al técnico igual que al confirmar, para que dos cambios simultáneos no tomen la misma franja.
        tecnicoRepositoryPort.buscarPorIdBloqueando(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));

        FranjaDisponible franja = calcularFranjas
                .calcular(cita.getEspecialidadId(), nuevoInicio.toLocalDate(), cita.getLatitud(), cita.getLongitud())
                .stream()
                .filter(f -> tecnicoId.equals(f.getTecnicoId()) && nuevoInicio.equals(f.getInicio()))
                .findFirst()
                .orElseThrow(() -> new FranjaNoDisponibleException(
                        "La nueva franja no está disponible para el técnico de tu cita"));

        EstadoCita estado = cita.getEstado();
        LocalDateTime inicioAnterior = cita.getFechaHoraInicio();
        cita.reprogramar(franja.getInicio(), franja.getFin());
        String motivo = "Reprogramada de " + inicioAnterior + " a " + franja.getInicio();
        Cita guardada = gestor.guardarConHistorial(cita, estado, correoCliente, motivo);

        gestor.notificarAlCliente(guardada, "Tu cita #" + guardada.getId() + " fue reprogramada para el "
                + guardada.getFechaHoraInicio());
        gestor.notificarAlTecnico(guardada, "El cliente reprogramó la cita #" + guardada.getId() + " para el "
                + guardada.getFechaHoraInicio());
        return guardada;
    }
}
