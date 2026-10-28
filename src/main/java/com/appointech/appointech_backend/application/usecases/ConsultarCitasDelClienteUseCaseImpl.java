package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCitasDelClienteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: el cliente consulta sus citas y el detalle (estado, técnico, diagnóstico, historial).
 * <p>Historias de usuario: SCRUM-18 (consultar estado e historial).
 */
@Service
public class ConsultarCitasDelClienteUseCaseImpl implements ConsultarCitasDelClienteUseCase {

    private final ResolverClienteAutenticado resolverCliente;
    private final CitaRepositoryPort citaRepositoryPort;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCita;
    private final HistorialEstadoCitaPort historialPort;

    public ConsultarCitasDelClienteUseCaseImpl(ResolverClienteAutenticado resolverCliente,
                                               CitaRepositoryPort citaRepositoryPort,
                                               ObtenerDetalleCitaUseCase obtenerDetalleCita,
                                               HistorialEstadoCitaPort historialPort) {
        this.resolverCliente = resolverCliente;
        this.citaRepositoryPort = citaRepositoryPort;
        this.obtenerDetalleCita = obtenerDetalleCita;
        this.historialPort = historialPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetalleCita> listar(String correoCliente) {
        Cliente cliente = resolverCliente.resolver(correoCliente);
        return citaRepositoryPort.listarPorCliente(cliente.getId()).stream()
                .map(obtenerDetalleCita::detallar)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleCitaCompleto detallar(String correoCliente, Long citaId) {
        Cliente cliente = resolverCliente.resolver(correoCliente);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> cliente.getId().equals(c.getClienteId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));
        return new DetalleCitaCompleto(obtenerDetalleCita.detallar(cita), historialPort.listarPorCita(citaId));
    }
}
