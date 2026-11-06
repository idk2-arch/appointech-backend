package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.CalificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCalificacionUseCase;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso (RF07): el cliente consulta la calificación de una de sus citas. */
@Service
public class ConsultarCalificacionUseCaseImpl implements ConsultarCalificacionUseCase {

    private final ResolverClienteAutenticado resolverCliente;
    private final CitaRepositoryPort citaRepositoryPort;
    private final CalificacionRepositoryPort calificacionRepositoryPort;

    public ConsultarCalificacionUseCaseImpl(ResolverClienteAutenticado resolverCliente,
                                            CitaRepositoryPort citaRepositoryPort,
                                            CalificacionRepositoryPort calificacionRepositoryPort) {
        this.resolverCliente = resolverCliente;
        this.citaRepositoryPort = citaRepositoryPort;
        this.calificacionRepositoryPort = calificacionRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Calificacion consultar(String correoCliente, Long citaId) {
        Cliente cliente = resolverCliente.resolver(correoCliente);
        citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> cliente.getId().equals(c.getClienteId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));
        return calificacionRepositoryPort.buscarPorCitaId(citaId)
                .orElseThrow(() -> new CalificacionNoEncontradaException(citaId));
    }
}
